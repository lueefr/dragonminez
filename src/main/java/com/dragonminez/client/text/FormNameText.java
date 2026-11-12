package com.dragonminez.client.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import java.util.*;

/** Screen rendering is scoped separately from HUD, chat, actionbar and world labels. */
public final class FormNameText {
    private static final Deque<Boolean> screens=new ArrayDeque<>();
    private static boolean drawing;
    private static boolean popup;
    public static void enterPopup() { popup=true; }
    public static void leavePopup() { popup=false; }
    public static boolean allowed() { return popup || modScreen(); }
    private FormNameText(){}
    public static com.dragonminez.common.stats.StatsData viewer() {
        var player=net.minecraft.client.Minecraft.getInstance().player;
        return player==null?null:com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE,player).orElse(null);
    }
    public static String variantKey(String key) {
        String prefix="race.dragonminez.";int marker=key.indexOf(".form.",prefix.length());
        if(!key.startsWith(prefix)||marker<0)return key;
        var data=viewer();if(data==null)return key;
        String race=key.substring(prefix.length(),marker);
        if(!race.equals(data.getCharacter().getRaceName())&&!race.equals("stack"))return key;
        String reference=key.substring(marker+6);int dot=reference.lastIndexOf('.');if(dot<1)return key;
        var target=com.dragonminez.api.forms.FormVariants.resolve(data,reference.substring(0,dot),reference.substring(dot+1));
        return com.dragonminez.common.util.FusionForms.formTranslationKey(race,target.group(),target.form());
    }
    public static void enterScreen(Object screen){
        boolean mod=screen instanceof com.dragonminez.client.gui.character.SkillsMenuScreen || screen instanceof com.dragonminez.client.gui.UtilityMenuScreen;
        screens.push(mod);
    }
    public static void leaveScreen(){if(!screens.isEmpty())screens.pop();}
    public static boolean inScreen(){return !screens.isEmpty();}
    public static boolean modScreen(){return screens.contains(true);}
    public static boolean drawing(){return drawing;}
    public static Style mark(Style style,int rgb){return ((FormNameStyle)style).dragonminez$copyColor(rgb);}
    public static Style unmark(Style style){return ((FormNameStyle)style).dragonminez$formColor()==null?style:((FormNameStyle)style).dragonminez$copyColor(null);}
    public static Integer color(Style style){return ((FormNameStyle)style).dragonminez$formColor();}
    public record Unstyled(FormattedCharSequence delegate) implements FormattedCharSequence {
        @Override public boolean accept(net.minecraft.util.FormattedCharSink sink){return delegate.accept(sink);}
    }
    public static FormattedCharSequence unstyled(FormattedCharSequence text){return new Unstyled(text);}
    public static int outline(int rgb){return 0.2126*(rgb>>16&255)+0.7152*(rgb>>8&255)+0.0722*(rgb&255)<128?0xFFFFFF:0x101010;}
    private record Glyph(int index,Style style,int codepoint){}
    private static FormattedCharSequence sequence(List<Glyph> glyphs,Integer forcedColor) {
        return sink->{for(var glyph:glyphs){Style style=unmark(glyph.style());if(forcedColor!=null)style=style.withColor(forcedColor);
            if(!sink.accept(glyph.index(),style,glyph.codepoint()))return false;}return true;};
    }
    public static Integer draw(Font font,FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f matrix,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light) {
        if(drawing||!allowed()||text instanceof Unstyled)return null;
        boolean[] named={false};
        text.accept((i,style,cp)->{named[0]=color(style)!=null;return !named[0];});
        if(modScreen()){
            var annotated=FormNamePalette.annotate(text);
            if(annotated!=null)text=annotated;else if(!named[0])return null;
        } else if(!named[0])return null;
        List<Glyph> glyphs=new ArrayList<>();text.accept((i,style,cp)->{glyphs.add(new Glyph(i,style,cp));return true;});
        drawing=true;
        try {

            float cursor=x;int result=(int)x;
            for(int start=0;start<glyphs.size();) {
                Integer rgb=color(glyphs.get(start).style());int end=start+1;
                while(end<glyphs.size()&&Objects.equals(rgb,color(glyphs.get(end).style())))end++;
                var run=glyphs.subList(start,end);var foreground=sequence(run,rgb);
                if(rgb!=null) {
                    int border=outline(rgb);var edge=sequence(run,border);
                    for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)
                        font.drawInBatch(edge,cursor+dx,y+dy,(color&0xFF000000)|border,false,matrix,buffers,mode,0,light);
                }
                result=font.drawInBatch(foreground,cursor,y,color,rgb==null&&shadow,matrix,buffers,mode,background,light);
                cursor+=font.width(foreground);start=end;
            }
            return result;
        } finally {drawing=false;}
    }
}
