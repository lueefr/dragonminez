package com.dragonminez.client.network;

public final class CreationScreen {
    private CreationScreen() {}
    public static void open() {
        var client=net.minecraft.client.Minecraft.getInstance();
        if(client.player==null)return;
        com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE,client.player)
                .ifPresent(data -> {
                    if(!data.getStatus().isHasCreatedCharacter())
                        client.setScreen(new com.dragonminez.client.gui.character.RaceSelectionScreen(data.getCharacter()));
                });
    }
}
