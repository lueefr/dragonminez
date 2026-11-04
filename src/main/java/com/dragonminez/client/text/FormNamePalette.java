package com.dragonminez.client.text;

import com.dragonminez.api.forms.FormAccess;
import com.dragonminez.api.lang.ConfigTranslations;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.FusionForms;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.util.StringDecomposer;
import java.util.*;
import java.util.regex.*;

/** Recognizes complete localized form names inside mod-authored prose, longest first. */
public final class FormNamePalette {
    private static Language language;
    private static Object forms,stacks;
    private static long revision=-1,generation=-1;
    private static String character="";
    private static Map<String,Integer> colors=Map.of();
    private static Pattern names;
    private static long renderRevision;
    private FormNamePalette(){}
    public static void invalidate(){language=null;}
    public static long revision(){refresh();return renderRevision;}
    private static void refresh() {
        var mc=Minecraft.getInstance();var player=mc.player;
        var data=player==null?null:StatsProvider.get(StatsCapability.INSTANCE,player).orElse(null);
        String race=data==null?"":data.getCharacter().getRaceName();
        String identity=race+"|"+(data==null?"":data.getCharacter().getHairColor()+"|"+(ConfigManager.getAttributes().hasForms()?ConfigManager.getAttributes().active(data).hashCode():0));
        var all=ConfigManager.getAllForms();var stack=ConfigManager.getAllStackForms();
        if(language==Language.getInstance()&&forms==all&&stacks==stack&&revision==ConfigTranslations.INSTANCE.revision()&&generation==ConfigManager.getFormConfigGeneration()&&character.equals(identity))return;
        language=Language.getInstance();forms=all;stacks=stack;revision=ConfigTranslations.INSTANCE.revision();generation=ConfigManager.getFormConfigGeneration();character=identity;
        renderRevision++;
        Map<String,Integer> next=new LinkedHashMap<>();
        // Same display name in several races uses the viewer's race; explicit components retain their exact owner.
        collect(next,race,ConfigManager.getAllFormsForRace(race),data);
        collect(next,"stack",stack,data);
        all.entrySet().stream().sorted(Map.Entry.comparingByKey()).filter(e->!e.getKey().equals(race)).forEach(e->collect(next,e.getKey(),e.getValue(),data));
        colors=Map.copyOf(next);
        var alternatives=next.keySet().stream().sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder())).map(Pattern::quote).toList();
        names=alternatives.isEmpty()?null:Pattern.compile("(?<![\\p{L}\\p{N}_])(?:"+String.join("|",alternatives)+")(?![\\p{L}\\p{N}_])");
    }
    private static void collect(Map<String,Integer> out,String race,Map<String,FormConfig> groups,StatsData data) {
        groups.forEach((group,config)->config.getForms().forEach((id,form)->{
            String key=FusionForms.formTranslationKey(race,group,id);Integer rgb=FormAccess.colorForKey(data,key);
            if(rgb==null||!language.has(key))return;
            String name=ChatFormatting.stripFormatting(language.getOrDefault(key)).strip();
            if(!name.isBlank())out.putIfAbsent(name,rgb);
        }));
    }

    private record Glyph(int index,Style style,int codepoint,int offset){}
    /** Recover identity from flattened labels only inside the mod's own screens. */
    public static net.minecraft.util.FormattedCharSequence annotate(net.minecraft.util.FormattedCharSequence text) {
        refresh();if(names==null)return null;
        List<Glyph> glyphs=new ArrayList<>();var plain=new StringBuilder();
        text.accept((i,style,cp)->{glyphs.add(new Glyph(i,style,cp,plain.length()));plain.appendCodePoint(cp);return true;});
        var matcher=names.matcher(plain);if(!matcher.find())return null;
        List<Match> matches=new ArrayList<>();do{matches.add(new Match(matcher.start(),matcher.end(),colors.get(matcher.group())));}while(matcher.find());
        return sink->{int match=0;for(var glyph:glyphs){
            while(match<matches.size()&&glyph.offset()>=matches.get(match).end())match++;
            var style=glyph.style();if(FormNameText.color(style)==null&&match<matches.size()&&glyph.offset()>=matches.get(match).start())style=FormNameText.mark(style,matches.get(match).color());
            if(!sink.accept(glyph.index(),style,glyph.codepoint()))return false;
        }return true;};
    }
    private record Match(int start,int end,int color){}
    public static <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor,Style style,String text) {
        if(FormNameText.color(style)!=null)return visitor.accept(style,text);
        refresh();if(names==null)return visitor.accept(style,text);
        String plain=ChatFormatting.stripFormatting(text);var matcher=names.matcher(plain);
        if(!matcher.find())return visitor.accept(style,text);
        List<Match> matches=new ArrayList<>();do{matches.add(new Match(matcher.start(),matcher.end(),colors.get(matcher.group())));}while(matcher.find());
        final int[] offset={0},match={0};final Optional<?>[] stopped={Optional.empty()};
        StringDecomposer.iterateFormatted(text,style,(index,glyphStyle,codepoint)->{
            while(match[0]<matches.size()&&offset[0]>=matches.get(match[0]).end())match[0]++;
            if(match[0]<matches.size()&&offset[0]>=matches.get(match[0]).start())glyphStyle=FormNameText.mark(glyphStyle,matches.get(match[0]).color());
            offset[0]+=Character.charCount(codepoint);
            Optional<T> result=visitor.accept(glyphStyle,new String(Character.toChars(codepoint)));stopped[0]=result;return result.isEmpty();
        });
        @SuppressWarnings("unchecked") Optional<T> result=(Optional<T>)stopped[0];return result;
    }
}
