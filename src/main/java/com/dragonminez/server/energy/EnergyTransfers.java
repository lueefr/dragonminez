package com.dragonminez.server.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;

/** Transfers atomically, including when a neighbor accepts only a partial amount. */
public final class EnergyTransfers {
    private EnergyTransfers() {}
    public static int move(EnergyStorage source, EnergyStorage target, int requested) {
        if (target == null || target == source || requested <= 0) return 0;
        try (var transaction = Transaction.openOuter()) {
            long available;
            try (var simulation = transaction.openNested()) {
                available = source.extract(requested, simulation);
            }
            long inserted = target.insert(available, transaction);
            long extracted = source.extract(inserted, transaction);
            if (extracted != inserted) return 0;
            transaction.commit();
            return (int) inserted;
        }
    }
}
