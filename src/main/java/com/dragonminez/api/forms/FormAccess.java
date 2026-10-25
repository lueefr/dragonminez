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
            if(forced)NetworkHandler.sendToPlayer(new RuleNoticeS2C(reason,color(data,group+"."+form)),player);
            else notice(player,reason,color(data,group+"."+form));
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
            if(hex!=null&&hex.isBlank())hex=data.getCharacter().getHairColor();
            if(hex != null && hex.matches("#?[0-9a-fA-F]{6}")) return Integer.parseInt(hex.replace("#",""),16);
        } catch (IllegalArgumentException ignored) {}
        return null;
    }
    public static Component join(List<Component> lines) {
        var result=Component.empty();
        for(Component line:lines){if(!result.getSiblings().isEmpty())result.append(Component.translatable("rules.dragonminez.separator"));result.append(line);}
        return result;
    }
    public static void notice(ServerPlayer player,Component reason,Integer color) {
        if (!com.dragonminez.common.network.PacketRateLimiter.allow(player.getUUID(),"rule_notice",player.serverLevel().getGameTime(),10)) return;
        NetworkHandler.sendToPlayer(new RuleNoticeS2C(reason,color),player);
    }
    private FormAccess() {}
}
