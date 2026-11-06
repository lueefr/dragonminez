package com.dragonminez.api.script;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local trusted server scripts return validated commands, never Java object references. */
public final class LuaProgram {
    public record Change(String meter, boolean additive, double value) {}
    public record Batch(List<Change> meters, List<com.google.gson.JsonObject> rewards) {
        public Batch { meters=List.copyOf(meters); rewards=List.copyOf(rewards); }
    }
    private final Prototype prototype;
    private final String name;
    public LuaProgram(String name, String source) {
        this.name = Objects.requireNonNull(name);
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65536 || source.indexOf('\u001b') >= 0) throw new IllegalArgumentException("Lua source must be text within 64 KiB");
        try { prototype = LuaC.instance.compile(new ByteArrayInputStream(bytes), name); }
        catch (IOException | RuntimeException ex) { throw new IllegalArgumentException("Cannot compile script " + name, ex); }
        evaluate(null, Map.of());
    }
    public List<Change> evaluate(String event, Map<String, Double> values) { return evaluateBatch(event,values).meters(); }
    public Batch evaluateBatch(String event, Map<String, Double> values) {
        var globals = new Globals();
        var packages = new LuaTable(); packages.set("loaded", new LuaTable()); globals.set("package", packages);
        globals.load(new BaseLib()); globals.load(new MathLib());
        for (String key : List.of("load", "loadfile", "dofile", "require", "collectgarbage", "print", "module")) globals.set(key, LuaValue.NIL);
        globals.get("math").set("random", LuaValue.NIL); globals.get("math").set("randomseed", LuaValue.NIL);
        var budget = new Budget(); globals.load(budget); globals.set("debug", LuaValue.NIL); globals.set("package", LuaValue.NIL);
        try {
            var module = new LuaClosure(prototype, globals).call().checktable();
            var callback = module.get("onEvent").checkfunction();
            if (event == null) return new Batch(List.of(),List.of());
            var context = new LuaTable(); context.set("event", event); var numbers = new LuaTable();
            values.forEach((key, value) -> { if (!Double.isFinite(value)) throw new IllegalArgumentException("Script inputs must be finite"); numbers.set(key, LuaValue.valueOf(value)); });
            context.set("values", numbers);
            var returned = callback.call(context);
            if (returned.isnil()) return new Batch(List.of(),List.of());
            var actions = returned.checktable(); int length = actions.length();
            if (length > 64) throw new IllegalArgumentException("At most 64 script commands");
            List<Change> changes = new ArrayList<>();
            List<com.google.gson.JsonObject> rewards=new ArrayList<>();
            for (int index = 1; index <= length; index++) {
                var action = actions.get(index).checktable();
                if(!action.get("reward").isnil()) {
                    if(!action.get("meter").isnil())throw new IllegalArgumentException("Script command cannot mix reward and meter");
                    rewards.add(json(action.get("reward"),0,new int[]{0}).getAsJsonObject());continue;
                }
                String meter = action.get("meter").checkjstring(), operation = action.get("operation").checkjstring();
                double value = action.get("value").checkdouble();
                if (!meter.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !Set.of("set", "add").contains(operation) || !Double.isFinite(value)) throw new IllegalArgumentException("Invalid script meter command");
                changes.add(new Change(meter, operation.equals("add"), value));
            }
            return new Batch(changes,rewards);
        } catch (BudgetStop ex) { throw new IllegalArgumentException("Script exceeded execution limit: " + name); }
        catch (LuaError ex) { throw new IllegalArgumentException("Script failed: " + name + ": " + ex.getMessage(), ex); }
    }
    private static com.google.gson.JsonElement json(LuaValue value,int depth,int[] nodes){
        if(depth>8||++nodes[0]>256)throw new IllegalArgumentException("Script reward exceeds structural limits");
        if(value.type()==LuaValue.TSTRING){String text=value.checkjstring();if(text.length()>4096)throw new IllegalArgumentException("Script reward string too long");return new com.google.gson.JsonPrimitive(text);}
        if(value.type()==LuaValue.TNUMBER){double number=value.checkdouble();if(!Double.isFinite(number))throw new IllegalArgumentException("Script reward number must be finite");return new com.google.gson.JsonPrimitive(number);}
        if(value.type()==LuaValue.TBOOLEAN)return new com.google.gson.JsonPrimitive(value.checkboolean());
        if(value.istable()){
            var table=value.checktable();
            if(table.length()>0){
                if(table.length()>64)throw new IllegalArgumentException("Script reward array too long");
                LuaValue key=LuaValue.NIL;int count=0;
                while(true){var next=table.next(key);key=next.arg1();if(key.isnil())break;if(!key.isint()||key.toint()<1||key.toint()>table.length()||++count>64)throw new IllegalArgumentException("Script reward arrays cannot mix object keys or gaps");}
                var array=new com.google.gson.JsonArray();for(int i=1;i<=table.length();i++)array.add(json(table.get(i),depth+1,nodes));return array;
            }
            var object=new com.google.gson.JsonObject();LuaValue key=LuaValue.NIL;
            while(true){var next=table.next(key);key=next.arg1();if(key.isnil())break;if(key.type()!=LuaValue.TSTRING)throw new IllegalArgumentException("Script reward needs string object keys");object.add(key.checkjstring(),json(next.arg(2),depth+1,nodes));}
            return object;
        }
        throw new IllegalArgumentException("Script reward must contain JSON values");
    }
    private static final class BudgetStop extends Error {}
    private static final class Budget extends DebugLib {
        private record Frame(LuaClosure closure, LuaValue[] stack) {}
        private final Deque<Frame> frames = new ArrayDeque<>();
        private int remaining = 20000;
        private final long deadline = System.nanoTime() + 100000000;
        @Override public void onCall(LuaFunction function) {
            enter(new Frame(null, null)); super.onCall(function);
        }
        @Override public void onCall(LuaClosure closure, Varargs args, LuaValue[] stack) {
            enter(new Frame(closure, stack)); super.onCall(closure, args, stack);
        }
        private void enter(Frame frame) {
            if (frames.size() >= 96) throw new BudgetStop(); frames.push(frame);
        }
        @Override public void onReturn() { super.onReturn(); if (!frames.isEmpty()) frames.pop(); }
        @Override public void onInstruction(int pc, Varargs args, int top) {
            if (--remaining < 0 || System.nanoTime() > deadline) throw new BudgetStop();
            var frame = frames.peek();
            if (frame != null && frame.closure() != null) {
                int instruction = frame.closure().p.code[pc];
                if (Lua.GET_OPCODE(instruction) == Lua.OP_CONCAT) {
                    long size = 0;
                    for (int index = Lua.GETARG_B(instruction); index <= Lua.GETARG_C(instruction); index++) {
                        var value = frame.stack()[index]; if (value != null) size += value.rawlen();
                    }
                    if (size > 65536) throw new BudgetStop();
                }
            }
            super.onInstruction(pc, args, top);
        }
    }
}
