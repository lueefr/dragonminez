package com.dragonminez.client.gui.dialogue;

import com.dragonminez.api.npc.CustomNpcRegistry;
import com.dragonminez.common.init.entities.questnpc.CustomNpcEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.*;
import com.dragonminez.common.quest.QuestRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class CustomNpcScreen extends Screen {
    private record Offer(Component label, Runnable action) {}
    private final CustomNpcEntity npc;
    private final List<Offer> offers=new ArrayList<>();
    private int page;
    private int pageSize;
    private CustomNpcScreen(CustomNpcEntity npc) { super(Component.translatable(npc.definition().name()));this.npc=npc; }
    public static void open(int entityId) {
        var mc=Minecraft.getInstance();
        if (mc.level!=null && mc.level.getEntity(entityId) instanceof CustomNpcEntity npc && npc.definition()!=null) mc.setScreen(new CustomNpcScreen(npc));
    }
    @Override protected void init() {
        offers.clear();
        var definition=npc.definition();
        if (definition==null) return;
        for (String skill:definition.offers().skills()) offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.skill",Component.translatable("skill.dragonminez."+skill)),
                () -> NetworkHandler.sendToServer(new UpdateSkillC2S(UpdateSkillC2S.SkillAction.PURCHASE,skill,0))));
        for (String id:definition.offers().quests()) {
            var quest=QuestRegistry.getClientQuest(id);
            Component name=quest==null?Component.literal(id):Component.translatable(quest.getTitle());
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.quest_start",name),
                    () -> NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.START,id,definition.id()))));
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.quest_turn_in",name),
                    () -> NetworkHandler.sendToServer(new QuestActionC2S(QuestActionC2S.ActionType.TURN_IN,id,definition.id()))));
        }
        for (int i=0;i<definition.offers().shop().size();i++) {
            int index=i;var trade=definition.offers().shop().get(i);
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(trade.item()));
            offers.add(new Offer(Component.translatable("gui.dragonminez.custom_npc.trade",item.getDescription(),trade.count(),trade.price()),
                    () -> NetworkHandler.sendToServer(new CustomNpcShopC2S(npc.getId(),index))));
        }
        pageSize=Math.max(1,(height-170)/24);
        int pages=Math.max(1,(offers.size()+pageSize-1)/pageSize);page=Math.min(page,pages-1);
        for (int i=page*pageSize;i<Math.min(offers.size(),(page+1)*pageSize);i++) {
            Offer offer=offers.get(i);
            addRenderableWidget(Button.builder(offer.label(),button -> offer.action().run()).bounds(width/2-150,110+(i%pageSize)*24,300,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"),button -> { page=Math.max(0,page-1);rebuildWidgets(); }).bounds(width/2-150,height-35,40,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),button -> onClose()).bounds(width/2-100,height-35,200,20).build());
        addRenderableWidget(Button.builder(Component.literal(">"),button -> { page=Math.min(pages-1,page+1);rebuildWidgets(); }).bounds(width/2+110,height-35,40,20).build());
    }
    @Override public void tick() {
        if (minecraft.player==null || !npc.isAlive() || minecraft.player.distanceToSqr(npc)>100) onClose();
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        super.render(graphics,mouseX,mouseY,partialTick);
        graphics.drawCenteredString(font,title,width/2,20,0xffffff);
        var definition=npc.definition();
        if (definition==null) return;
        int y=40;
        for (String dialogue:definition.dialogues()) {
            for (var line:font.split(Component.translatable(dialogue),Math.min(500,width-40))) {
                if (y>90) break;
                graphics.drawString(font,line,width/2-Math.min(500,width-40)/2,y,0xdddddd);y+=10;
            }
        }
    }
    @Override public boolean isPauseScreen() { return false; }
}
