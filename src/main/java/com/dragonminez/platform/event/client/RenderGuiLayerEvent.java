package com.dragonminez.platform.event.client;

import com.dragonminez.platform.event.Event;
import com.dragonminez.platform.event.ICancellableEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public abstract class RenderGuiLayerEvent extends Event {
    private final Identifier name;
    private final GuiGraphicsExtractor graphics;
    private final DeltaTracker partialTick;
    protected RenderGuiLayerEvent(Identifier name, GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        this.name = name; this.graphics = graphics; this.partialTick = partialTick;
    }
    public Identifier getName() { return name; }
    public GuiGraphicsExtractor getGuiGraphics() { return graphics; }
    public DeltaTracker getPartialTick() { return partialTick; }
    public static final class Pre extends RenderGuiLayerEvent implements ICancellableEvent {
        public Pre(Identifier name, GuiGraphicsExtractor graphics, DeltaTracker partialTick) { super(name, graphics, partialTick); }
    }
    public static final class Post extends RenderGuiLayerEvent {
        public Post(Identifier name, GuiGraphicsExtractor graphics, DeltaTracker partialTick) { super(name, graphics, partialTick); }
    }
}
