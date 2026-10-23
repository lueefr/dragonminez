import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Verifies native descriptors and invocation sites used by gameplay bridge mixins. */
public final class NativeGameplayLayoutTest {
    static ClassNode type(String name) throws Exception {
        var type = new ClassNode();
        try (var stream = ClassLoader.getSystemResourceAsStream(name + ".class")) {
            if (stream == null) throw new AssertionError("Missing target " + name);
            new ClassReader(stream).accept(type, 0);
        }
        return type;
    }
    static MethodNode method(ClassNode type, String name, String descriptor) {
        return type.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError("Missing native method " + name + descriptor));
    }
    static void call(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode invoke && invoke.owner.equals(owner)
                    && invoke.name.equals(name) && invoke.desc.equals(descriptor)) count++;
        if (count != 1) throw new AssertionError("Expected exactly one hook in " + method.name + ": " + name + ", found " + count);
    }
    public static void main(String[] args) throws Exception {
        String owner = "net/minecraft/world/entity/LivingEntity";
        var living = type(owner);
        method(living, "causeFallDamage", "(DFLnet/minecraft/world/damagesource/DamageSource;)Z");
        method(living, "removeEffect", "(Lnet/minecraft/core/Holder;)Z");
        method(living, "removeAllEffects", "()Z");
        method(living, "onEffectAdded", "(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V");
        method(living, "collectEquipmentChanges", "()Ljava/util/Map;");
        if (living.fields.stream().noneMatch(f -> f.name.equals("lastEquipmentItems") && f.desc.equals("Ljava/util/Map;")))
            throw new AssertionError("Previous equipment snapshot missing");
        call(method(living, "tickEffects", "()V"), owner, "onEffectsRemoved", "(Ljava/util/Collection;)V");
        call(method(living, "startUsingItem", "(Lnet/minecraft/world/InteractionHand;)V"),
                "net/minecraft/world/item/ItemStack", "getUseDuration", "(Lnet/minecraft/world/entity/LivingEntity;)I");
        call(method(living, "completeUsingItem", "()V"), "net/minecraft/world/item/ItemStack", "finishUsingItem",
                "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;");
        method(type("net/minecraft/world/entity/Mob"), "setTarget", "(Lnet/minecraft/world/entity/LivingEntity;)V");
        method(living, "getDimensions", "(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;");
        method(type("net/minecraft/world/level/entity/PersistentEntitySectionManager"), "addEntity",
                "(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z");
        method(type("net/minecraft/world/entity/projectile/Projectile"), "hitTargetOrDeflectSelf",
                "(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/entity/projectile/ProjectileDeflection;");
        call(method(type("net/minecraft/world/level/ServerExplosion"), "explode", "()I"),
                "net/minecraft/world/level/ServerExplosion", "calculateExplodedPositions", "()Ljava/util/List;");
        method(type("net/minecraft/client/KeyboardHandler"), "keyPress", "(JILnet/minecraft/client/input/KeyEvent;)V");
        method(type("net/minecraft/client/MouseHandler"), "onScroll", "(JDD)V");
        method(type("net/minecraft/client/multiplayer/ClientPacketListener"), "handleRespawn",
                "(Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;)V");
        var client = type("net/minecraft/client/Minecraft");
        method(client, "startAttack", "()Z");
        method(client, "continueAttack", "(Z)V");
        method(client, "startUseItem", "()V");
        method(client, "pickBlockOrEntity", "()V");
        var camera = type("net/minecraft/client/Camera");
        method(camera, "calculateFov", "(F)F");
        call(method(camera, "update", "(Lnet/minecraft/client/DeltaTracker;)V"), "net/minecraft/client/Camera", "alignWithEntity", "(F)V");
        var fog = type("net/minecraft/client/renderer/fog/FogRenderer");
        method(fog, "computeFogColor", "(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V");
        method(fog, "setupFog", "(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;");
        System.out.println("Native gameplay hook descriptors: item use, equipment, target, fall, effects, entity admission/size, projectile, explosion, client input/respawn, camera and fog verified.");
    }
}
