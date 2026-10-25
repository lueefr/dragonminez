package com.dragonminez.client.gui.hair;

import com.dragonminez.api.forms.FormHair;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Data-driven form entries; the existing editor still owns all strand editing tools. */
public final class FormHairScreen extends Screen {
    private final Screen previous;
    private final Character character;
    private final List<String> forms;
    private int page;
    public static Screen open(Screen previous,Character character){return FormHair.editable(character.getRaceName()).isEmpty()?new HairEditorScreen(previous,character):new FormHairScreen(previous,character);}
    private FormHairScreen(Screen previous,Character character){super(Component.translatable("gui.dragonminez.form_hair.title"));this.previous=previous;this.character=character;forms=new ArrayList<>(FormHair.editable(character.getRaceName()).keySet());}
    @Override protected void init(){
        int x=width/2-110,y=55;
        addRenderableWidget(Button.builder(Component.translatable("gui.dragonminez.form_hair.base"),b->minecraft.setScreen(new HairEditorScreen(this,character))).bounds(x,y,220,20).build());
        int count=Math.max(1,(height-160)/24);
        for(int i=page*count;i<Math.min(forms.size(),(page+1)*count);i++){
            String id=forms.get(i);var form=FormHair.get(character.getRaceName(),id);
            var label=FormHair.name(character.getRaceName(),id);
            addRenderableWidget(Button.builder(label,b->minecraft.setScreen(new HairEditorScreen(this,character,id))).bounds(x,85+(i-page*count)*24,220,20).build());
        }
        addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.translatable("text.dragonminez.formhairscreen.dabd3aff76"),b->{if(page>0){page--;rebuildWidgets();}}).bounds(x,height-55,35,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(x+40,height-55,140,20).build());
        addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.translatable("text.dragonminez.formhairscreen.62b67e1f68"),b->{if((page+1)*count<forms.size()){page++;rebuildWidgets();}}).bounds(x+185,height-55,35,20).build());
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);graphics.drawCenteredString(font,title,width/2,25,0xffffff);}
    @Override public void onClose(){minecraft.setScreen(previous);}
}
