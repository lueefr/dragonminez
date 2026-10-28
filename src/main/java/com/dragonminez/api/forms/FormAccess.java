package com.dragonminez.api.forms;

import com.dragonminez.api.rules.Conditions;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RuleNoticeS2C;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/** Use-only conditions. Unlock and purchase checks deliberately do not consult this API. */
public final class FormAccess {
    public static List<Component> missing(StatsData data, String group, String form, boolean stack) {
        FormConfig.FormData definition = stack ? ConfigManager.getStackForm(group, form)
                : ConfigManager.getForm(data.getCharacter().getRaceName(), group, form);
        return definition == null ? List.of() : Conditions.missing(new Conditions.Context(data, group+"."+form), definition.getUse());
    }
    public static boolean allow(StatsData data, String group, String form, boolean stack, boolean forced) {
        if (data == null || data.getPlayer() == null || data.getPlayer().level().isClientSide
                || group == null || form == null || group.isBlank() || form.isBlank()) return true;
        List<Component> missing = missing(data,group,form,stack);
        if (missing.isEmpty()) return true;
        if (data.getPlayer() instanceof ServerPlayer player) {
            Component reason = Component.translatable(forced ? "rules.dragonminez.use_warning" : "rules.dragonminez.use_denied",
                    name(data,group+"."+form), join(missing));
            notice(player,reason,color(data,group+"."+form));
        }
        return forced;
    }
    public static Component name(StatsData data, String ref) {
        Component name = FormHair.name(data.getCharacter().getRaceName(),ref);
        Integer color = color(data,ref);
        return color == null ? name : name.copy().withStyle(style -> style.withColor(color));
    }
    public static Integer color(StatsData data,String ref) {
        try {
            var form = FormHair.get(data.getCharacter().getRaceName(),ref);
            String hex = form == null ? null : form.getHairColor();
            if(hex==null||hex.isBlank())hex=data.getCharacter().getHairColor();
            if(hex != null && hex.matches("#?[0-9a-fA-F]{6}")) return Integer.parseInt(hex.replace("#",""),16);
        } catch (IllegalArgumentException ignored) {}
        return null;
    }
    /** Resolve identity keys, never translated display strings: works in every locale. */
    public static Integer colorForKey(StatsData data,String key) {
        String prefix="race.dragonminez.";
        if(!key.startsWith(prefix))return null;
        int separator=key.indexOf(".form.",prefix.length());
        if(separator<0)return null;
        String race=key.substring(prefix.length(),separator),ref=key.substring(separator+6);
        int dot=ref.lastIndexOf('.');if(dot<=0)return null;
        var definition=ConfigManager.getForm(race,ref.substring(0,dot),ref.substring(dot+1));
        if(definition==null)definition=ConfigManager.getStackForm(ref.substring(0,dot),ref.substring(dot+1));
        if(definition==null)return null;
        String hex=definition.getHairColor();
        if((hex==null||hex.isBlank())&&data!=null)hex=data.getCharacter().getHairColor();
        return hex!=null&&hex.matches("#?[0-9a-fA-F]{6}")?Integer.parseInt(hex.replace("#",""),16):null;
    }
    public static boolean isMasteryNotice(Component message) {
        if(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text) {
            if(text.getKey().equals("rules.dragonminez.mastery")||text.getKey().equals("message.dragonminez.form.free_transform_mastery"))return true;
            for(Object argument:text.getArgs())if(argument instanceof Component child&&isMasteryNotice(child))return true;
        }
        return message.getSiblings().stream().anyMatch(FormAccess::isMasteryNotice);
    }
    public static Component coloredNames(Component message,StatsData data) {
        net.minecraft.network.chat.MutableComponent result;
        if(message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text) {
            Object[] args=text.getArgs().clone();
            for(int i=0;i<args.length;i++)if(args[i] instanceof Component child)args[i]=coloredNames(child,data);
            result=net.minecraft.network.chat.MutableComponent.create(new net.minecraft.network.chat.contents.TranslatableContents(text.getKey(),text.getFallback(),args));
            result.setStyle(message.getStyle());
            Integer color=colorForKey(data,text.getKey());if(color!=null)result.withStyle(style->style.withColor(color));
        }else result=message.plainCopy().setStyle(message.getStyle());
        for(Component sibling:message.getSiblings())result.append(coloredNames(sibling,data));
        return result;
    }
    public static Component join(List<Component> lines) {
        var result=Component.empty();
        for(Component line:lines){if(!result.getSiblings().isEmpty())result.append(Component.translatable("rules.dragonminez.separator"));result.append(line);}
        return result;
    }
    public static void notice(ServerPlayer player,Component reason,Integer color) {
        if (!com.dragonminez.common.network.PacketRateLimiter.allow(player.getUUID(),"rule_notice",player.serverLevel().getGameTime(),10)) return;
        var data=com.dragonminez.common.stats.StatsProvider.get(com.dragonminez.common.stats.StatsCapability.INSTANCE,player).orElse(null);
        if(data!=null)reason=coloredNames(reason,data);
        if(isMasteryNotice(reason)){player.displayClientMessage(reason,true);return;}
        NetworkHandler.sendToPlayer(new RuleNoticeS2C(reason,color),player);
    }
    private FormAccess() {}
}
