import com.dragonminez.platform.registry.DeferredRegister;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RegistryMigrationTest {
    public static void main(String[] args) {
        var key = ResourceKey.<String>createRegistryKey(Identifier.parse("test:values"));
        var registry = new MappedRegistry<String>(key, Lifecycle.stable());
        var entries = DeferredRegister.create(registry, "test");
        AtomicInteger calls = new AtomicInteger();
        var first = entries.register("first", () -> { calls.incrementAndGet(); return "value"; });
        if (first.isBound()) throw new AssertionError("Factory ran before registration");
        try { first.get(); throw new AssertionError("Unbound access allowed"); }
        catch (IllegalStateException expected) { }
        try { entries.register("first", () -> "duplicate"); throw new AssertionError("Duplicate accepted"); }
        catch (IllegalArgumentException expected) { }
        entries.register();
        entries.register();
        if (calls.get()!=1 || !first.isBound() || !first.get().equals("value")) throw new AssertionError();
        if (!registry.containsKey(Identifier.parse("test:first"))) throw new AssertionError("Not in native registry");
        if (!first.is(ResourceKey.create(key,Identifier.parse("test:first")))) throw new AssertionError();
        if (!first.is(registry.get(Identifier.parse("test:first")).orElseThrow())) throw new AssertionError();
        registry.freeze();
        if (!registry.getValue(Identifier.parse("test:first")).equals("value")) throw new AssertionError("Native registry value lost after freeze");
        if (!first.get().equals("value")) throw new AssertionError("Registry freeze invalidated the reference");
        System.out.println("Native registry migration: stable holders, explicit phase, actual registry binding, duplicate rejection and single factory evaluation passed.");
    }
}
