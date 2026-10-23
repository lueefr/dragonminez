from pathlib import Path
root=Path('src/main/java/com/dragonminez')
for p in (root/'common/init/block').rglob('*.java'):
 s=p.read_text()
 if 'net.neoforged.neoforge.energy.IEnergyStorage' in s:
  s=s.replace('import net.neoforged.neoforge.energy.IEnergyStorage;', 'import team.reborn.energy.api.EnergyStorage;\nimport com.dragonminez.server.energy.EnergyTransfers;')
  s=s.replace('public IEnergyStorage getEnergyStorage()', 'public StarEnergyStorage getEnergyStorage()')
  s=s.replace('IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite());', 'EnergyStorage target = EnergyStorage.SIDED.find(level, neighbor, dir.getOpposite());')
  s=s.replace('if (target != null && target.canReceive()) {\n\t\t\t\tint sent = target.receiveEnergy(Math.min(energyStorage.getEnergyStored(), 256), false);\n\t\t\t\tenergyStorage.extractEnergy(sent, false);\n\t\t\t}', 'EnergyTransfers.move(energyStorage, target, 256);')
  s=s.replace('IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);', 'EnergyStorage energy = EnergyStorage.SIDED.find(level, pos, side);')
  s=s.replace('return energy.receiveEnergy(amount, false);', 'return EnergyTransfers.move(energyStorage, energy, amount);')
  s=s.replace('\n\t\tenergyStorage.extractEnergy(distributed, false);', '')
 s=s.replace('import net.neoforged.neoforge.capabilities.Capabilities;\n', '')
 s=s.replace('level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side)', 'team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, side)')
 p.write_text(s)
p=root/'common/events/ModCommonEvents.java';s=p.read_text()
s=s.replace('import net.neoforged.neoforge.capabilities.Capabilities;\n','').replace('import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;\n','')
a=s.index('\t@SubscribeEvent\n\tpublic static void onRegisterCapabilities(')
s=s[:a]+'''    public static void registerMachineStorage() {
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be.getItemHandler(), side),
                MainBlockEntities.KIKONO_STATION_BE.get());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(be.getItemHandler(), side),
                MainBlockEntities.FUEL_GENERATOR_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.KIKONO_STATION_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.FUEL_GENERATOR_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.ENERGY_CABLE_BE.get());
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.getEnergyStorage(), MainBlockEntities.GRAVITY_DEVICE_BE.get());
    }

}
'''
p.write_text(s)
p=root/'common/DMZCommon.java';s=p.read_text().replace('ModCommonEvents.registerAttributes();','ModCommonEvents.registerMachineStorage();\n\t\tModCommonEvents.registerAttributes();');p.write_text(s)
