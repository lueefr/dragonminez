import com.dragonminez.platform.nbt.NbtCompat;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import java.util.UUID;

public final class NbtMigrationTest {
    public static void main(String[] args) {
        CompoundTag tag = new CompoundTag();
        UUID id=UUID.randomUUID();
        tag.putIntArray("old_uuid",UUIDUtil.uuidToIntArray(id));
        if (!NbtCompat.hasUUID(tag,"old_uuid") || !NbtCompat.getUUID(tag,"old_uuid").equals(id)) throw new AssertionError("Legacy UUID lost");
        NbtCompat.putUUID(tag,"new_uuid",id);
        if (!NbtCompat.getUUID(tag,"new_uuid").equals(id)) throw new AssertionError();
        tag.putInt("number",42);
        if (!NbtCompat.contains(tag,"number",99) || NbtCompat.contains(tag,"number",Tag.TAG_STRING)) throw new AssertionError("Wrong NBT type check");
        ListTag values=new ListTag();values.add(StringTag.valueOf("entry"));tag.put("values",values);
        if (NbtCompat.getList(tag,"values",Tag.TAG_STRING).size()!=1) throw new AssertionError();
        if (!NbtCompat.getList(tag,"values",Tag.TAG_COMPOUND).isEmpty()) throw new AssertionError("Wrong element type accepted");
        if (NbtCompat.hasUUID(tag,"missing")) throw new AssertionError();
        System.out.println("NBT migration: legacy/new UUID compatibility, numeric type checks and typed-list fallback passed.");
    }
}
