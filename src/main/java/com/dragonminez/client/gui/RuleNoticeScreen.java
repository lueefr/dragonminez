package com.dragonminez.client.gui;

import com.dragonminez.client.util.PanelSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Tutorial-style modal: every colored span gets its own contrasting outline. */
public final class RuleNoticeScreen extends Screen {
    private final Screen parent;
    private final Component message;
    private final int color;
    private List<FormattedCharSequence> lines;
    private int panelWidth,panelHeight,left,top,scroll,visibleLines;
    public RuleNoticeScreen(Screen parent,Component message,Integer color) {
        super(Component.translatable("rules.dragonminez.notice"));
        this.parent=parent;this.message=message;
        this.color=color==null?message.visit((style,text)->style.getColor()==null?java.util.Optional.<Integer>empty():
                java.util.Optional.of(style.getColor().getValue()),Style.EMPTY).orElse(0xFFFFFF):color;
    }
    public static void open(Component message,Integer color) {
        Minecraft mc=Minecraft.getInstance();
        Screen parent=mc.screen instanceof RuleNoticeScreen notice?notice.parent:mc.screen;
        mc.setScreen(new RuleNoticeScreen(parent,message,color));
    }
    @Override protected void init() {
        panelWidth=Math.min(340,width-20);
        var styled=message.copy().withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("dragonminez","smooth")));
        lines=font.split(styled,panelWidth-28);
        panelHeight=Math.min(height-20,48+lines.size()*11);visibleLines=Math.max(1,(panelHeight-48)/11);left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        addRenderableWidget(Button.builder(Component.translatable("gui.ok").withStyle(Style.EMPTY.withFont(
                ResourceLocation.fromNamespaceAndPath("dragonminez","smooth"))),b->onClose())
                .bounds(width/2-40,top+panelHeight-29,80,20).build());
    }
    public static int outline(int rgb) {
        double luminance=0.2126*(rgb>>16&255)+0.7152*(rgb>>8&255)+0.0722*(rgb&255);
        return luminance<128?0xFFFFFF:0x101010;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics,mouseX,mouseY,partialTick);
        graphics.fill(0,0,width,height,0x88000000);
        PanelSkin.SMALL_PANEL.draw(graphics,left,top,panelWidth,panelHeight);
        int y=top+14;
        for(var line:lines.subList(scroll,Math.min(lines.size(),scroll+visibleLines))) {
            final int row=y;final int[] x={left+14};
            line.accept((index,style,codepoint)->{
                int rgb=style.getColor()==null?color:style.getColor().getValue();
                Component glyph=Component.literal(new String(Character.toChars(codepoint))).withStyle(style.withColor(rgb));
                Component border=glyph.copy().withStyle(style.withColor(outline(rgb)));
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)
                    graphics.drawString(font,border,x[0]+dx,row+dy,0xFF000000|outline(rgb),false);
                graphics.drawString(font,glyph,x[0],row,0xFF000000|rgb,false);
                x[0]+=font.width(glyph);return true;
            });
            y+=11;
        }
        for(var renderable:renderables)renderable.render(graphics,mouseX,mouseY,partialTick);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        scroll=Math.max(0,Math.min(Math.max(0,lines.size()-visibleLines),scroll-(int)Math.signum(vertical)));return true;
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
}
