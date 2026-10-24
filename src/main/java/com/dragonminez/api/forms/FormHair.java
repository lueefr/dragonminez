package com.dragonminez.api.forms;

import com.dragonminez.common.config.*;
import java.util.*;

/** Only explicit editHair:true entries get a player-owned editor entry. */
public final class FormHair {
    public static Map<String,FormConfig.FormData> editable(String race){
        Map<String,FormConfig.FormData> entries=new TreeMap<>();
        ConfigManager.getAllFormsForRace(race).forEach((group,c)->collect(entries,group,c));
        ConfigManager.getAllStackForms().forEach((group,c)->collect(entries,group,c));return Collections.unmodifiableMap(entries);
    }
    private static void collect(Map<String,FormConfig.FormData> entries,String group,FormConfig config){config.getForms().forEach((name,form)->{if(Boolean.TRUE.equals(form.getEditHair()))entries.put(group+"."+name,form);});}
    public static FormConfig.FormData get(String race,String id){String[] ref=com.dragonminez.api.rules.Conditions.form(id);var group=ConfigManager.getFormGroup(race,ref[0]);if(group==null)group=ConfigManager.getStackFormGroup(ref[0]);return group==null?null:group.getForm(ref[1]);}
    public static net.minecraft.network.chat.Component name(String race,String id){
        var form=get(race,id);String[] ref=com.dragonminez.api.rules.Conditions.form(id);
        String owner=ConfigManager.getAllStackForms().containsKey(ref[0])?"stack":race;
        return net.minecraft.network.chat.Component.translatableWithFallback("race.dragonminez."+owner+".form."+id,form==null?id:form.getName());
    }
    private FormHair(){}
}
