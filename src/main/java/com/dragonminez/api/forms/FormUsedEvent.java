package com.dragonminez.api.forms;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Emitted after an actual server-side activation, never for previews or client sync. */
public final class FormUsedEvent extends Event {
    private final ServerPlayer player;
    private final String form;
    private final boolean first;
    public FormUsedEvent(ServerPlayer player,String form,boolean first) {this.player=player;this.form=form;this.first=first;}
    public ServerPlayer getPlayer(){return player;}
    public String getForm(){return form;}
    public boolean isFirst(){return first;}
}
