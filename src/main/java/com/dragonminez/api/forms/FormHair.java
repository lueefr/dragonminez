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
    private static String[] reference(String id){
        int dot=id==null?-1:id.indexOf('.');
        if(dot<=0)throw new IllegalArgumentException("Expected group.form: "+id);
        String group=id.substring(0,dot),form=id.substring(dot+1);
        com.dragonminez.api.rules.Conditions.form(com.dragonminez.common.util.FusionForms.baseGroup(group)+"."+form);
        return new String[]{group,form};
    }
    public static FormConfig.FormData get(String race,String id){
        String[] ref=reference(id);var group=ConfigManager.getFormGroup(race,ref[0]);
        if(group==null)group=ConfigManager.getStackFormGroup(ref[0]);
        return group==null?null:group.getForm(ref[1]);
    }
    public static net.minecraft.network.chat.Component name(String race,String id){
        String[] ref=reference(id);
        String owner=ConfigManager.getAllStackForms().containsKey(ref[0])?"stack":race;
        return net.minecraft.network.chat.Component.translatable(
            com.dragonminez.common.util.FusionForms.formTranslationKey(owner,ref[0],ref[1]));
    }
    private FormHair(){}
}
