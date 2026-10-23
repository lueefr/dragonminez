package com.dragonminez.common.training;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import com.dragonminez.common.util.RuntimeRegistries;

public final class MusicDiscHelper {
    private MusicDiscHelper() {}
    public static JukeboxSong song(Item item) {
        if (item == null) return null;
        var playable = item.getDefaultInstance().get(DataComponents.JUKEBOX_PLAYABLE);
        return playable == null ? null : playable.song().unwrap(RuntimeRegistries.access()).map(holder -> holder.value()).orElse(null);
    }
}
