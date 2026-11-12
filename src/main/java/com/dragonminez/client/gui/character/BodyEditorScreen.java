package com.dragonminez.client.gui.character;

import com.dragonminez.common.stats.character.BodyShape;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dedicated proportions panel retaining the character creation model preview. */
public final class BodyEditorScreen extends Screen {
    private final CharacterCustomizationScreen parent;
    private final Character character;
    private final Runnable sync;
    public BodyEditorScreen(CharacterCustomizationScreen parent, Character character, Runnable sync) {
        super(text("title")); this.parent=parent; this.character=character; this.sync=sync;
    }
    private static Component text(String key) { return Component.translatable("gui.dragonminez.body_editor."+key); }
    @Override protected void init() {
        int x=16, y=32, size=Math.min(190,width/2-24);
        for (BodyShape.Part part:BodyShape.Part.values()) {
            addRenderableWidget(new AbstractSliderButton(x,y,size,20,text(part.name().toLowerCase()),character.getBodyShape().get(part)-.5) {
                { updateMessage(); }
                @Override protected void updateMessage() { setMessage(Component.translatable("gui.dragonminez.body_editor.value",text(part.name().toLowerCase()),Math.round((value+.5)*100))); }
                @Override protected void applyValue() { character.getBodyShape().set(part,(float)value+.5f); sync.run(); }
            });
            y+=22;
        }
        addRenderableWidget(Button.builder(frameName(),button -> {
            character.getBodyShape().frame(character.getBodyShape().frame()==1 ? -1 : character.getBodyShape().frame()+1);
            button.setMessage(frameName()); sync.run();
        }).bounds(x,y,size,20).build());
        y+=22;
        addRenderableWidget(new AbstractSliderButton(x,y,size,20,text("bust"),character.getBoobScale()/1.25) {
            { updateMessage(); }
            @Override protected void updateMessage() { setMessage(Component.translatable("gui.dragonminez.body_editor.value",text("bust"),Math.round(value*125))); }
            @Override protected void applyValue() { character.setBoobScale((float)value*1.25f); sync.run(); }
        });
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),button -> onClose()).bounds(x,height-28,size,20).build());
    }
    private Component frameName() { return Component.translatable("gui.dragonminez.body_editor.frame_value",text(character.getBodyShape().frame()<0?"skin":character.getBodyShape().frame()==0?"steve":"alex")); }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        parent.render(graphics,-1000,-1000,partial);
        graphics.fill(8,8,Math.min(214,width/2),height-8,0xEE171B24);
        graphics.drawString(font,title,16,18,0xFFFFFF);
        super.render(graphics,mouseX,mouseY,partial);
    }
    @Override public void onClose() { sync.run(); minecraft.setScreen(parent); }
}
