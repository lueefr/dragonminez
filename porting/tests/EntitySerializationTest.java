import com.dragonminez.platform.nbt.NbtValues;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class EntitySerializationTest {
    public static void main(String[] args) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.putString("vanilla", "retained");
        CompoundTag parent = new CompoundTag(); parent.putInt("quest", 42);
        CompoundTag nested = new CompoundTag(); nested.putString("id", "test:content"); parent.put("nested", nested);
        ListTag list = new ListTag(); list.add(StringTag.valueOf("entry")); parent.put("list", list);
        NbtValues.write(output, parent);
        CompoundTag child = new CompoundTag(); child.putBoolean("custom", true); NbtValues.write(output, child);
        CompoundTag saved = output.buildResult();
        if (!saved.getStringOr("vanilla", "").equals("retained") || saved.getIntOr("quest", 0) != 42 || !saved.getBooleanOr("custom", false)) throw new AssertionError("Parent/native data overwritten");
        var input = TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, saved);
        CompoundTag restored = NbtValues.read(input);
        if (!restored.equals(saved)) throw new AssertionError("Flat-key round trip changed entity data");
        restored.remove("quest");
        if (saved.getIntOr("quest", 0) != 42) throw new AssertionError("Loading mutated source data");
        System.out.println("Entity serialization: native ValueInput/ValueOutput, flat keys, parent/child composition, nested tags and source isolation passed.");
    }
}
