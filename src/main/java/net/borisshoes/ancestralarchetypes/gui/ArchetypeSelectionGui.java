package net.borisshoes.ancestralarchetypes.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.*;
import net.borisshoes.borislib.gui.GraphicalItem;
import net.borisshoes.borislib.gui.GuiHelper;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class ArchetypeSelectionGui extends SimpleGui {
   
   private final int[][] dynamicSlots = {{},{3},{1,5},{1,3,5},{0,2,4,6},{1,2,3,4,5},{0,1,2,4,5,6},{0,1,2,3,4,5,6}};
   private final SubArchetype subArchetype;
   private final boolean showOnly;
   private boolean menuOnClose;
   private final HashMap<Integer,SubArchetype> map;
   private int page = 1;
   private final int numPages = Math.max(1, (int)Math.ceil((float)ArchetypeRegistry.ARCHETYPES.size() / 7.0));
   
   public ArchetypeSelectionGui(ServerPlayer player, SubArchetype subArchetype, boolean showOnly){
      super(getScreenType(subArchetype), player, false);
      this.subArchetype = subArchetype;
      this.map = new HashMap<>();
      this.showOnly = showOnly;
      setTitle(Component.translatable("text.ancestralarchetypes.gui_title"));
      if(this.subArchetype != null) menuOnClose = true;
      build();
   }
   
   private static MenuType<?> getScreenType(SubArchetype sub){
      if(sub == null) return MenuType.GENERIC_9x6;
      ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
      abilities.addAll(Arrays.asList(sub.getAbilities()));
      abilities.addAll(Arrays.asList(sub.getArchetype().getAbilities()));
      abilities.removeIf(a1 -> abilities.stream().anyMatch(a2 -> a2.overrides(a1)));
      int size = abilities.size();
      if(size <= 7){
         return MenuType.GENERIC_9x3;
      }else if(size <= 14){
         return MenuType.GENERIC_9x4;
      }else if(size <= 21){
         return MenuType.GENERIC_9x5;
      }else{
         return MenuType.GENERIC_9x6;
      }
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action){
      PlayerArchetypeData profile = profile(player);
      if(subArchetype == null){
         if(this.map.containsKey(index)){
            ArchetypeSelectionGui gui = new ArchetypeSelectionGui(player,this.map.get(index),this.showOnly);
            gui.open();
         }else if(index == 4 && profile.getSubArchetype() != null && !showOnly){
            profile.changeArchetype(player,null);
            this.close();
         }
      }else{
         if(index == (getSize()-9) || index == 4){
            this.close();
         }else if(index == (getSize()-5) && profile.getSubArchetype() != subArchetype && !showOnly){
            profile.changeArchetype(player,subArchetype);
            this.menuOnClose = false;
            this.close();
         }
      }
      return true;
   }
   
   public void build(){
      if(subArchetype == null){
         buildMainMenu();
      }else{
         buildArchetypeMenu();
      }
   }
   
   private void buildArchetypeMenu(){
      PlayerArchetypeData profile = profile(player);
      GuiHelper.outlineGUI(this,subArchetype.getColor(), Component.empty());
      
      GuiElementBuilder subArchetypeItem = GuiElementBuilder.from(subArchetype.getDisplayItem()).hideDefaultTooltip();
      subArchetypeItem.setName(subArchetype.getName().withColor(subArchetype.getColor()));
      subArchetypeItem.addLoreLine(subArchetype.getDescription().withStyle(TextUtils.getClosestFormatting(subArchetype.getColor())));
      setSlot(4,subArchetypeItem);
      
      ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
      abilities.addAll(Arrays.asList(subArchetype.getAbilities()));
      abilities.addAll(Arrays.asList(subArchetype.getArchetype().getAbilities()));
      abilities.removeIf(a1 -> abilities.stream().anyMatch(a2 -> a2.overrides(a1)));
      
      int rows = (int) Math.ceil(abilities.size()/7.0);
      for(int i = 0; i < rows; i++){
         int[] dynamicSlot = dynamicSlots[abilities.size() < 7*(i+1) ? abilities.size() - (7*i) : 7];
         for(int j = 0; j < 7; j++){
            setSlot(1+(9*(i+1)+j), GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.PAGE_BG,subArchetype.getColor())).hideTooltip());
         }
         
         for(int j = 0; j < dynamicSlot.length; j++){
            int index = i*7 + j;
            int offset = 1 + dynamicSlot[j];
            ArchetypeAbility ability = abilities.get(index);
            GuiElementBuilder abilityItem = GuiElementBuilder.from(ability.displayStack()).hideDefaultTooltip();
            abilityItem.setName(ability.getName().withColor(subArchetype.getColor()));
            abilityItem.addLoreLine(ability.getDescription().withStyle(TextUtils.getClosestFormatting(subArchetype.getColor())));
            setSlot((i+1)*9+offset,abilityItem);
         }
      }
      
      if(!showOnly){
         setSlot(getSize()-5, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.CONFIRM)).hideDefaultTooltip()
               .setName(Component.translatable("text.ancestralarchetypes.confirm_selection").withStyle(ChatFormatting.GREEN))
               .addLoreLine(Component.translatable("text.ancestralarchetypes.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC))
         );
      }
      
      setSlot(getSize()-9, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.LEFT_ARROW)).hideDefaultTooltip()
            .setName(Component.translatable("text.ancestralarchetypes.return").withStyle(ChatFormatting.GOLD))
      );
   }
   
   
   private void buildMainMenu(){
      this.map.clear();
      PlayerArchetypeData profile = profile(player);
      GuiHelper.outlineGUI(this,0x20c3e0, Component.empty());
      setSlot(18,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_LEFT_CONNECTOR,0x20c3e0)).hideTooltip());
      setSlot(26,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_RIGHT_CONNECTOR,0x20c3e0)).hideTooltip());
      
      GameProfile gameProfile = player.getGameProfile();
      GuiElementBuilder head = new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(gameProfile,player.level().getServer());
      head.setName(Component.translatable("text.ancestralarchetypes.gui_title").withStyle(ChatFormatting.AQUA));
      head.addLoreLine(TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.gui_subtitle_1").withStyle(ChatFormatting.DARK_AQUA)));
      if(profile.getSubArchetype() != null && !showOnly){
         head.addLoreLine(Component.empty());
         head.addLoreLine(TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.gui_subtitle_2").withStyle(ChatFormatting.RED)));
         head.addLoreLine(TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)));
      }
      setSlot(4,head);
      
      if(numPages > 1){
         setSlot(45,createPrevPageItem());
         setSlot(53,createNextPageItem());
      }
      
      List<Archetype> archetypes = getArchetypesForPage(page);
      int[] dynamicSlot = dynamicSlots[archetypes.size()];
      for(int i = 0; i < dynamicSlot.length; i++){
         int offset = 1 + dynamicSlot[i];
         
         Archetype archetype = archetypes.get(i);
         GuiElementBuilder archetypeItem = GuiElementBuilder.from(archetype.getDisplayItem()).hideDefaultTooltip();
         archetypeItem.setName(archetype.getName().withColor(archetype.getColor()));
         archetypeItem.addLoreLine(archetype.getDescription().withStyle(TextUtils.getClosestFormatting(archetype.getColor())));
         setSlot(9+offset,archetypeItem);
         setSlot(18+offset,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_HORIZONTAL,archetype.getColor())).hideTooltip());
         
         List<SubArchetype> subArchetypes = ArchetypeRegistry.SUBARCHETYPES.stream().filter(sub -> sub.getArchetype() == archetype).toList();
         for(int j = 0; j < 3; j++){
            GuiElementBuilder subArchetypeItem;
            if(j < subArchetypes.size()){
               SubArchetype subArchetype = subArchetypes.get(j);
               subArchetypeItem = GuiElementBuilder.from(subArchetype.getDisplayItem()).hideDefaultTooltip();
               subArchetypeItem.setName(subArchetype.getName().withColor(subArchetype.getColor()));
               subArchetypeItem.addLoreLine(subArchetype.getDescription().withStyle(TextUtils.getClosestFormatting(subArchetype.getColor())));
               subArchetypeItem.addLoreLine(Component.literal(""));
               subArchetypeItem.addLoreLine(Component.translatable("text.ancestralarchetypes.gui_subtitle_1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
               this.map.put(27+offset+(9*j),subArchetype);
            }else{
               subArchetypeItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.PAGE_BG,archetype.getColor())).hideTooltip();
            }
            setSlot(27+offset+(9*j), subArchetypeItem);
         }
      }
   }
   
   private GuiElementBuilder createNextPageItem() {
      GuiElementBuilder nextPage = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.RIGHT_ARROW));
      nextPage.setName(Component.translatable("gui.borislib.next_page_title", this.page, this.numPages).withColor(ChatFormatting.AQUA.getColor().intValue()));
      nextPage.addLoreLine(Component.translatable("text.borislib.two_elements", Component.translatable("gui.borislib.click").withColor(ChatFormatting.GREEN.getColor().intValue()), Component.translatable("gui.borislib.next_page_sub").withColor(ChatFormatting.DARK_AQUA.getColor().intValue())));
      nextPage.setCallback((clickType) -> {
         if (this.page < this.numPages) {
            ++this.page;
            this.buildMainMenu();
         }
      });
      return nextPage;
   }
   
   private GuiElementBuilder createPrevPageItem() {
      GuiElementBuilder prevPage = GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.LEFT_ARROW));
      prevPage.setName(Component.translatable("gui.borislib.prev_page_title", this.page, this.numPages).withColor(ChatFormatting.AQUA.getColor().intValue()));
      prevPage.addLoreLine(Component.translatable("text.borislib.two_elements", Component.translatable("gui.borislib.click").withColor(ChatFormatting.GREEN.getColor().intValue()), Component.translatable("gui.borislib.prev_page_sub").withColor(ChatFormatting.DARK_AQUA.getColor().intValue())));
      prevPage.setCallback((clickType) -> {
         if (this.page > 1) {
            --this.page;
            this.buildMainMenu();
         }
      });
      return prevPage;
   }
   
   private List<Archetype> getArchetypesForPage(int page){
      int lastInd = Math.min(7*page-1,ArchetypeRegistry.ARCHETYPES.size()-1);
      int firstInd = Math.max(lastInd-6,0);
      List<Archetype> archetypes = new ArrayList<>();
      for(int i = firstInd; i <= lastInd; i++){
         archetypes.add(ArchetypeRegistry.ARCHETYPES.byId(i));
      }
      return archetypes;
   }
   
   @Override
   public void onClose(){
      if(this.menuOnClose){
         ArchetypeSelectionGui gui = new ArchetypeSelectionGui(player,null, this.showOnly);
         gui.open();
      }
   }
}
