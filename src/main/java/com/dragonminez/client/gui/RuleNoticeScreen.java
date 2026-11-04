package com.dragonminez.client.gui;

import com.dragonminez.client.util.PanelSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.dragonminez.Reference;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Menu notification: every colored span gets its own contrasting outline. */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class RuleNoticeScreen extends Screen {
    private static RuleNoticeScreen active;
    private final Screen parent;
    private final long expiry;
    private final Component message;
    private final int color;
    private List<FormattedCharSequence> lines;
    private int panelWidth,panelHeight,left,top,scroll,visibleLines;
    public RuleNoticeScreen(Screen parent,Component message,Integer color) {
        super(Component.translatable("rules.dragonminez.notice"));
        this.parent=parent;this.message=message;
        this.expiry=net.minecraft.Util.getMillis()+5000;
        this.color=color==null?message.visit((style,text)->style.getColor()==null?java.util.Optional.<Integer>empty():
                java.util.Optional.of(style.getColor().getValue()),Style.EMPTY).orElse(0xFFFFFF):color;
    }
    public static void open(Component message,Integer color) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.screen==null) {
            active=null;
            mc.gui.setOverlayMessage(message,false);
            return;
        }
        active=new RuleNoticeScreen(mc.screen,message,color);
        active.init(mc,mc.screen.width,mc.screen.height);
    }
    @SubscribeEvent public static void drawNotice(ScreenEvent.Render.Post event) {
        if(active==null)return;
        if(active.parent!=event.getScreen()||net.minecraft.Util.getMillis()>=active.expiry){active=null;return;}
        active.render(event.getGuiGraphics(),event.getMouseX(),event.getMouseY(),event.getPartialTick());
    }
    @SubscribeEvent public static void dismissNotice(ScreenEvent.MouseButtonPressed.Pre event) {
        active=null;
    }
    @SubscribeEvent public static void tickNotice(ClientTickEvent.Post event) {
        if(active!=null&&(Minecraft.getInstance().screen!=active.parent||net.minecraft.Util.getMillis()>=active.expiry))active=null;
    }
    @Override protected void init() {
        panelWidth=Math.max(1,Math.min(340,width-20));
        var styled=message.copy().withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("dragonminez","smooth")));
        lines=font.split(styled,Math.max(1,panelWidth-28));
        panelHeight=Math.min(Math.max(1,height-20),28+lines.size()*11);
        visibleLines=Math.max(1,(panelHeight-28)/11);
        left=(width-panelWidth)/2;top=Math.max(0,height-42-panelHeight);
    }
    public static int outline(int rgb) {
        double luminance=0.2126*(rgb>>16&255)+0.7152*(rgb>>8&255)+0.0722*(rgb&255);
        return luminance<128?0xFFFFFF:0x101010;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        PanelSkin.SMALL_PANEL.draw(graphics,left,top,panelWidth,panelHeight);
        int y=top+14;
        for(var line:lines.subList(scroll,Math.min(lines.size(),scroll+visibleLines))) {
            final int row=y;final int[] x={left+14};
            line.accept((index,style,codepoint)->{
                int rgb=style.getColor()==null?color:style.getColor().getValue();
                Component glyph=Component.literal(new String(Character.toChars(codepoint))).withStyle(com.dragonminez.client.text.FormNameText.unmark(style).withColor(rgb));
                Component border=glyph.copy().withStyle(com.dragonminez.client.text.FormNameText.unmark(style).withColor(outline(rgb)));
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)
                    graphics.drawString(font,com.dragonminez.client.text.FormNameText.unstyled(border.getVisualOrderText()),x[0]+dx,row+dy,0xFF000000|outline(rgb),false);
                graphics.drawString(font,com.dragonminez.client.text.FormNameText.unstyled(glyph.getVisualOrderText()),x[0],row,0xFF000000|rgb,false);
                x[0]+=font.width(glyph);return true;
            });
            y+=11;
        }
    }
}
