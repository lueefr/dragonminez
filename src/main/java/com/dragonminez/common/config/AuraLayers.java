package com.dragonminez.common.config;

import com.dragonminez.api.rules.Conditions;
import com.google.gson.JsonObject;
import java.util.*;
import lombok.Getter;

/** Optional layer lists replace legacy main/extra definitions without changing old files. */
public final class AuraLayers {
    private AuraLayers() {}
    @Getter
    public static final class Layer {
        private String id;
        private String operation = "add";
        private String target;
        private String type2d = "kakarot", type3d = "smooth", color = "#FFFFFF";
        private float scale = 1, opacity = 1;
        private int order;
        private JsonObject when = new JsonObject();
        private JsonObject sealedWhen;
        private double sealSeconds = 0.4;
        private JsonObject dynamic = new JsonObject();
        private transient Map<String, com.dragonminez.api.runtime.DynamicValue> expressions;
        private transient boolean failed;
        public record Values(float scale, float opacity) {}
        private synchronized Map<String, com.dragonminez.api.runtime.DynamicValue> compile() {
            if(expressions!=null)return expressions;
            if(dynamic==null)throw new IllegalArgumentException("Missing dynamic aura object");
            Map<String,com.dragonminez.api.runtime.DynamicValue> result=new LinkedHashMap<>();
            for(var entry:dynamic.entrySet()){
                if(!Set.of("scale","opacity").contains(entry.getKey()))throw new IllegalArgumentException("Unknown dynamic aura field: "+entry.getKey());
                var expression=com.dragonminez.api.runtime.DynamicValue.compile(entry.getValue());
                for(String source:expression.dependencies()){
                    if(source.startsWith("meter:"))com.dragonminez.api.content.ContentProgress.requireId(source.substring(6));
                    else if(!Set.of("state.formTicks","state.stackTicks","self.healthPercent","attack.charging","attack.active","attack.recovery","attack.ticks","attack.charge").contains(source))throw new IllegalArgumentException("Unknown dynamic aura source: "+source);
                }
                result.put(entry.getKey(),expression);
            }
            expressions=Map.copyOf(result);return expressions;
        }
        public Values values(Map<String,Double> context) {
            if(failed)return new Values(scale,opacity);
            try{
                var expressions=compile();
                float size=expressions.containsKey("scale")?(float)Math.clamp(expressions.get("scale").evaluate(context),.05,8):scale;
                float alpha=expressions.containsKey("opacity")?(float)Math.clamp(expressions.get("opacity").evaluate(context),0,1):opacity;
                return new Values(size,alpha);
            }catch(RuntimeException ex){
                failed=true;com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON,"Dynamic aura layer {} failed: {}. Static appearance used until reload.",id,ex.getMessage());
                return new Values(scale,opacity);
            }
        }
        private Emitter emitter;
        private FormConfig.Aura3DStyle style = new FormConfig.Aura3DStyle();
    }
    @Getter
    public static final class Emitter {
        private int count=1, interval=2, lifetime=30;
        private float radius=0.6f, height=1, speed=0.03f, rise=0.04f, size=1;
        public void validate() {
            if(count<0||count>8||interval<1||interval>200||lifetime<1||lifetime>200)throw new IllegalArgumentException("Invalid aura emitter count/interval/lifetime");
            for(float value:new float[]{radius,height,speed,rise,size})if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite aura emitter parameter");
            if(radius<0||radius>4||height<0||height>4||speed<0||speed>1||rise < -1||rise>1||size<0.05||size>8)throw new IllegalArgumentException("Aura emitter parameter outside bounds");
        }
    }
    public static <T> void apply(List<T> layers, Layer definition, T value, java.util.function.Function<T, String> identity) {
        if (!definition.operation.equals("add")) layers.removeIf(layer -> identity.apply(layer).equals(definition.target));
        if (!definition.operation.equals("remove") && value != null) layers.add(value);
    }
    public static void validate(List<Layer> layers) {
        if (layers == null) return;
        if (layers.size() > 16) throw new IllegalArgumentException("At most 16 aura layers per definition");
        Set<String> ids = new HashSet<>();
        for (var layer : layers) {
            if (layer == null || layer.id == null || !layer.id.matches("[a-z0-9_.:/-]{1,128}") || !ids.add(layer.id)) throw new IllegalArgumentException("Aura layers need unique stable IDs");
            if (!Set.of("add", "replace", "remove").contains(layer.operation == null ? "" : layer.operation)) throw new IllegalArgumentException("Invalid aura layer operation");
            if (layer.operation.equals("add") ? layer.target != null : layer.target == null || !layer.target.matches("(?:base|stack)/[a-z0-9_.:/-]{1,128}")) throw new IllegalArgumentException("Aura replace/remove needs a base/id or stack/id target; add has no target");
            if (layer.color == null || !layer.color.matches("#[0-9a-fA-F]{6}") || !Float.isFinite(layer.scale) || layer.scale < 0.05 || layer.scale > 8 || !Float.isFinite(layer.opacity) || layer.opacity < 0 || layer.opacity > 1) throw new IllegalArgumentException("Invalid aura color, scale or opacity");
            if (layer.type2d == null || !layer.type2d.matches("[a-z0-9_/-]{1,64}") || !Set.of("smooth", "sparking", "god", "userPreference").contains(layer.type3d)) throw new IllegalArgumentException("Invalid aura renderer type");
            if (layer.when == null || layer.style == null) throw new IllegalArgumentException("Missing aura conditions/style");
            layer.compile();
            if(layer.emitter!=null)layer.emitter.validate();
            Conditions.validate(layer.when);
            if (layer.sealedWhen != null) Conditions.validate(layer.sealedWhen);
            if (!Double.isFinite(layer.sealSeconds) || layer.sealSeconds < 0.01 || layer.sealSeconds > 10) throw new IllegalArgumentException("Aura sealSeconds must be 0.01..10");
        }
    }
}
