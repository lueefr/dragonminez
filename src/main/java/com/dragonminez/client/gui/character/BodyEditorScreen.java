package com.dragonminez.client.gui.character;

import com.dragonminez.client.gui.character.util.ScaledScreen;
import com.dragonminez.client.gui.hair.HairEditorUi;
import com.dragonminez.common.stats.character.BodyShape;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Uses creation's panorama and model, with the same scaled, unblurred editor theme. */
public final class BodyEditorScreen extends ScaledScreen {
    private final CharacterCustomizationScreen parent;
    private final Character character;
    private final Runnable sync;
    private int panelWidth;
    public BodyEditorScreen(CharacterCustomizationScreen parent, Character character, Runnable sync) {
        super(text("title")); this.parent=parent; this.character=character; this.sync=sync;
    }
    private static Component text(String key) { return HairEditorUi.tr("gui.dragonminez.body_editor."+key); }
    private int group;
    private static final String[] GROUPS={"general","head_group","torso_group","arms_group","legs_group"};
    @Override protected void init() {
        panelWidth=Math.min(240,getUiWidth()/2-12);
        int left=20,width=panelWidth-20,tabWidth=(width-8)/3;
        for(int index=0;index<GROUPS.length;index++) {
            final int selected=index;
            addRenderableWidget(button(left+(index%3)*(tabWidth+4),42+(index/3)*23,tabWidth,text(GROUPS[index]),b -> {
                group=selected;rebuildWidgets();
            }));
        }
        BodyShape.Part[] parts=switch(group) {
            case 1 -> new BodyShape.Part[]{BodyShape.Part.HEAD,BodyShape.Part.HEAD_WIDTH,BodyShape.Part.HEAD_DEPTH,BodyShape.Part.HEAD_HEIGHT};
            case 2 -> new BodyShape.Part[]{BodyShape.Part.TORSO_WIDTH,BodyShape.Part.TORSO_DEPTH,BodyShape.Part.TORSO_HEIGHT};
            case 3 -> new BodyShape.Part[]{BodyShape.Part.ARM_WIDTH,BodyShape.Part.ARM_DEPTH,BodyShape.Part.ARMS};
            case 4 -> new BodyShape.Part[]{BodyShape.Part.LEG_WIDTH,BodyShape.Part.LEG_DEPTH,BodyShape.Part.LEGS};
            default -> new BodyShape.Part[]{BodyShape.Part.HEIGHT};
        };
        int gap=Math.min(32,Math.max(24,(getUiHeight()-145)/4));
        for(int index=0;index<parts.length;index++) {
            final BodyShape.Part current=parts[index];
            addRenderableWidget(new ShapeSlider(left,96+index*gap,width,text(current.name().toLowerCase(java.util.Locale.ROOT)),character.getBodyShape().get(current)-.5) {
                @Override protected void applyValue() {character.getBodyShape().set(current,(float)value+.5f);sync.run();}
            });
        }
        if(group==0) {
            addRenderableWidget(new ShapeSlider(left,96+gap,width,text("bust"),character.getBoobScale()/1.25) {
                @Override protected void updateMessage() {setMessage(HairEditorUi.tr("gui.dragonminez.body_editor.value",text("bust"),Math.round(value*125)));}
                @Override protected void applyValue() {character.setBoobScale((float)value*1.25f);sync.run();}
            });
            addRenderableWidget(button(left,100+gap*2,width,frameName(),b -> {
                character.getBodyShape().frame(character.getBodyShape().frame()==1?-1:character.getBodyShape().frame()+1);
                b.setMessage(frameName());sync.run();
            }));
        }
        int footer=getUiHeight()-36;
        addRenderableWidget(button(left,footer,(width-6)/2,text("reset"),b -> {
            for(var part:BodyShape.Part.values())character.getBodyShape().set(part,1);
            character.getBodyShape().frame(-1);character.setBoobScale(1);sync.run();rebuildWidgets();
        }));
        addRenderableWidget(button(left+(width+6)/2,footer,(width-6)/2,HairEditorUi.tr("gui.done"),b -> onClose()));
    }
    private abstract class ShapeSlider extends AbstractSliderButton {
        private final Component label;
        ShapeSlider(int x,int y,int width,Component label,double value) {
            super(x,y,width,26,label,value);this.label=label;updateMessage();
        }
        @Override protected void updateMessage() { setMessage(HairEditorUi.tr("gui.dragonminez.body_editor.value",label,Math.round((value+.5)*100))); }
        @Override public void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
            graphics.drawString(font,getMessage(),getX(),getY(),HairEditorUi.TEXT,false);
            HairEditorUi.track(graphics,getX(),getY()+14,width,9);
            HairEditorUi.trackFill(graphics,getX(),getY()+14,(int)(value*(width-5))+5,9);
            HairEditorUi.outline(graphics,getX()+(int)(value*(width-5)),getY()+13,5,11,isHoveredOrFocused()?HairEditorUi.TITLE:HairEditorUi.DIVIDER);
        }
    }
    private Button button(int x,int y,int width,Component label,Button.OnPress action) {
        return new Button(x,y,width,20,label,action,supplier -> supplier.get()) {
            @Override public void renderWidget(GuiGraphics g,int mx,int my,float partial) {
                HairEditorUi.button(g,font,getMessage(),getX(),getY(),getWidth(),getHeight(),isHoveredOrFocused(),false,active);
            }
        };
    }
    private Component frameName() { return HairEditorUi.tr("gui.dragonminez.body_editor.frame_value",text(character.getBodyShape().frame()<0?"skin":character.getBodyShape().frame()==0?"steve":"alex")); }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        parent.renderBodyEditorPreview(graphics,partial);
        beginUiScale(graphics);
        HairEditorUi.panel(graphics,10,10,panelWidth,getUiHeight()-20);
        HairEditorUi.header(graphics,font,title,10,10,panelWidth);
        super.render(graphics,(int)toUiX(mouseX),(int)toUiY(mouseY),partial);
        endUiScale(graphics);
    }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if (toUiX(x)>panelWidth+10 && button==0) { setDragging(true); return true; }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(toUiX(x)>panelWidth+10 && button==0) {
            parent.rotateBodyEditorPreview(dx/getUiScale(),dy/getUiScale()); return true;
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public void onClose() { sync.run(); minecraft.setScreen(parent); }
}
