import com.dragonminez.server.energy.StarEnergyStorage;
import com.dragonminez.server.energy.EnergyTransfers;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.nbt.CompoundTag;

public final class EnergyMigrationTest {
    static final class Storage extends StarEnergyStorage {
        int changes;
        Storage(int capacity, int transfer) { super(capacity, transfer); }
        @Override public void onEnergyChanged() { changes++; }
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var source = new Storage(150, 15);
        var target = new Storage(100, 100);
        source.setEnergy(100);
        int changes = source.changes;
        check(source.extractEnergy(40, true) == 15 && source.getEnergyStored() == 100
                && source.changes == changes, "Simulation modified or dirtied source");
        check(source.receiveEnergy(40, true) == 15 && source.getEnergyStored() == 100
                && source.changes == changes, "Simulation modified or dirtied target");
        check(EnergyTransfers.move(source, target, 100) == 15, "Source transfer limit ignored");
        check(source.getEnergyStored() == 85 && target.getEnergyStored() == 15, "Energy conservation failed");
        target.setEnergy(97);
        check(EnergyTransfers.move(source, target, 100) == 3 && source.getEnergyStored() == 82
                && target.getEnergyStored() == 100, "Partial transfer lost or duplicated energy");
        changes = source.changes;
        check(EnergyTransfers.move(source, target, 100) == 0 && source.changes == changes, "Full target dirtied source");
        try (var transaction = Transaction.openOuter()) {
            source.extract(10, transaction);
        }
        check(source.getEnergyStored() == 82 && source.changes == changes, "Aborted transaction did not roll back");
        check(EnergyTransfers.move(source, source, 10) == 0, "Self transfer accepted");
        var tag = new CompoundTag(); source.saveNBT(tag);
        var restored = new Storage(150, 15); restored.loadNBT(tag);
        check(restored.getEnergyStored() == 82, "Energy persistence failed");
        restored.setEnergy(-1); check(restored.getEnergyStored() == 0, "Negative energy accepted");
        tag.putInt("energy", 999); restored.loadNBT(tag);
        check(restored.getEnergyStored() == 150, "Loaded energy exceeded capacity");
        System.out.println("Energy: rollback, simulation, source limits, partial transfer conservation and persistence passed.");
    }
}
