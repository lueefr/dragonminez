import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Checks the native bytecode locations used by the damage mixins against the actual target jar. */
public final class NativeDamageLayoutTest {
    public static void main(String[] args) throws Exception {
        for (String owner : new String[]{"net/minecraft/world/entity/LivingEntity", "net/minecraft/world/entity/player/Player"}) {
            ClassNode type = new ClassNode();
            try (var stream = ClassLoader.getSystemResourceAsStream(owner + ".class")) {
                if (stream == null) throw new AssertionError("Missing target class: " + owner);
                new ClassReader(stream).accept(type, 0);
            }
            MethodNode method = type.methods.stream().filter(candidate -> candidate.name.equals("actuallyHurt")
                    && candidate.desc.equals("(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V")).findFirst().orElseThrow();
            AbstractInsnNode afterAbsorption = null;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals("setAbsorptionAmount") && call.desc.equals("(F)V")) {
                    afterAbsorption = instruction.getNext(); break;
                }
            }
            if (afterAbsorption == null) throw new AssertionError("Absorption hook missing: " + owner);
            int matchingLoads = 0; boolean foundGuard = false;
            for (AbstractInsnNode instruction = afterAbsorption; instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof VarInsnNode variable && variable.getOpcode() == Opcodes.FLOAD && variable.var == 3) {
                    if (matchingLoads++ == 1) {
                        AbstractInsnNode next = instruction.getNext();
                        while (next != null && next.getOpcode() < 0) next = next.getNext();
                        if (next == null || next.getOpcode() != Opcodes.FCONST_0) throw new AssertionError("Damage hook no longer precedes zero-damage guard: " + owner);
                        foundGuard = true; break;
                    }
                }
            }
            if (!foundGuard) throw new AssertionError("Final damage variable/guard missing: " + owner);
        }
        System.out.println("Native damage hook layout: both native damage implementations retain absorption-before-event and event-before-zero-guard locations.");
    }
}
