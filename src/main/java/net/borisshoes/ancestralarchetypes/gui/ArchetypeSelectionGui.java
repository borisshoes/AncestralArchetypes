package net.borisshoes.ancestralarchetypes.gui;

import com.mojang.authlib.GameProfile;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.Archetype;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.gui.GraphicalItem;
import net.borisshoes.borislib.gui.GuiHelper;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
   
   public ArchetypeSelectionGui(ServerPlayerEntity player, SubArchetype subArchetype, boolean showOnly){
      super(getScreenType(subArchetype), player, false);
      this.subArchetype = subArchetype;
      this.map = new HashMap<>();
      this.showOnly = showOnly;
      setTitle(Text.translatable("text.ancestralarchetypes.gui_title"));
      if(this.subArchetype != null) menuOnClose = true;
      build();
   }
   
   private static ScreenHandlerType<?> getScreenType(SubArchetype sub){
      if(sub == null) return ScreenHandlerType.GENERIC_9X6;
      ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
      abilities.addAll(Arrays.asList(sub.getAbilities()));
      abilities.addAll(Arrays.asList(sub.getArchetype().getAbilities()));
      abilities.removeIf(a1 -> abilities.stream().anyMatch(a2 -> a2.overrides(a1)));
      int size = abilities.size();
      if(size <= 7){
         return ScreenHandlerType.GENERIC_9X3;
      }else if(size <= 14){
         return ScreenHandlerType.GENERIC_9X4;
      }else if(size <= 21){
         return ScreenHandlerType.GENERIC_9X5;
      }else{
         return ScreenHandlerType.GENERIC_9X6;
      }
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      IArchetypeProfile profile = profile(player);
      if(subArchetype == null){
         if(this.map.containsKey(index)){
            ArchetypeSelectionGui gui = new ArchetypeSelectionGui(player,this.map.get(index),this.showOnly);
            gui.open();
         }else if(index == 4 && profile.getSubArchetype() != null && !showOnly){
            profile.changeArchetype(null);
            this.close();
         }
      }else{
         if(index == (getSize()-9) || index == 4){
            this.close();
         }else if(index == (getSize()-5) && profile.getSubArchetype() != subArchetype && !showOnly){
            profile.changeArchetype(subArchetype);
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
      IArchetypeProfile profile = profile(player);
      GuiHelper.outlineGUI(this,subArchetype.getColor(), Text.empty());
      
      GuiElementBuilder subArchetypeItem = GuiElementBuilder.from(subArchetype.getDisplayItem()).hideDefaultTooltip();
      subArchetypeItem.setName(subArchetype.getName().withColor(subArchetype.getColor()));
      subArchetypeItem.addLoreLine(subArchetype.getDescription().formatted(TextUtils.getClosestFormatting(subArchetype.getColor())));
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
            GuiElementBuilder abilityItem = GuiElementBuilder.from(ability.getDisplayStack()).hideDefaultTooltip();
            abilityItem.setName(ability.getName().withColor(subArchetype.getColor()));
            abilityItem.addLoreLine(ability.getDescription().formatted(TextUtils.getClosestFormatting(subArchetype.getColor())));
            setSlot((i+1)*9+offset,abilityItem);
         }
      }
      
      if(!showOnly){
         setSlot(getSize()-5, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.CONFIRM)).hideDefaultTooltip()
               .setName(Text.translatable("text.ancestralarchetypes.confirm_selection").formatted(Formatting.GREEN))
               .addLoreLine(Text.translatable("text.ancestralarchetypes.warning").formatted(Formatting.DARK_RED,Formatting.ITALIC))
         );
      }
      
      setSlot(getSize()-9, GuiElementBuilder.from(GraphicalItem.with(GraphicalItem.LEFT_ARROW)).hideDefaultTooltip()
            .setName(Text.translatable("text.ancestralarchetypes.return").formatted(Formatting.GOLD))
      );
   }
   
   
   private void buildMainMenu(){
      this.map.clear();
      IArchetypeProfile profile = profile(player);
      GuiHelper.outlineGUI(this,0x20c3e0, Text.empty());
      setSlot(18,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_LEFT_CONNECTOR,0x20c3e0)).hideTooltip());
      setSlot(26,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_RIGHT_CONNECTOR,0x20c3e0)).hideTooltip());
      
      GameProfile gameProfile = player.getGameProfile();
      GuiElementBuilder head = new GuiElementBuilder(Items.PLAYER_HEAD).setSkullOwner(gameProfile,player.getServer());
      head.setName(Text.translatable("text.ancestralarchetypes.gui_title").formatted(Formatting.AQUA));
      head.addLoreLine(TextUtils.removeItalics(Text.translatable("text.ancestralarchetypes.gui_subtitle_1").formatted(Formatting.DARK_AQUA)));
      if(profile.getSubArchetype() != null && !showOnly){
         head.addLoreLine(Text.empty());
         head.addLoreLine(TextUtils.removeItalics(Text.translatable("text.ancestralarchetypes.gui_subtitle_2").formatted(Formatting.RED)));
         head.addLoreLine(TextUtils.removeItalics(Text.translatable("text.ancestralarchetypes.warning").formatted(Formatting.DARK_RED, Formatting.ITALIC)));
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
         archetypeItem.addLoreLine(archetype.getDescription().formatted(TextUtils.getClosestFormatting(archetype.getColor())));
         setSlot(9+offset,archetypeItem);
         setSlot(18+offset,GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.MENU_HORIZONTAL,archetype.getColor())).hideTooltip());
         
         List<SubArchetype> subArchetypes = ArchetypeRegistry.SUBARCHETYPES.stream().filter(sub -> sub.getArchetype() == archetype).toList();
         for(int j = 0; j < 3; j++){
            GuiElementBuilder subArchetypeItem;
            if(j < subArchetypes.size()){
               SubArchetype subArchetype = subArchetypes.get(j);
               subArchetypeItem = GuiElementBuilder.from(subArchetype.getDisplayItem()).hideDefaultTooltip();
               subArchetypeItem.setName(subArchetype.getName().withColor(subArchetype.getColor()));
               subArchetypeItem.addLoreLine(subArchetype.getDescription().formatted(TextUtils.getClosestFormatting(subArchetype.getColor())));
               subArchetypeItem.addLoreLine(Text.literal(""));
               subArchetypeItem.addLoreLine(Text.translatable("text.ancestralarchetypes.gui_subtitle_1").formatted(Formatting.DARK_PURPLE,Formatting.ITALIC));
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
      nextPage.setName(Text.translatable("gui.borislib.next_page_title", this.page, this.numPages).withColor(Formatting.AQUA.getColorValue().intValue()));
      nextPage.addLoreLine(Text.translatable("text.borislib.two_elements", Text.translatable("gui.borislib.click").withColor(Formatting.GREEN.getColorValue().intValue()), Text.translatable("gui.borislib.next_page_sub").withColor(Formatting.DARK_AQUA.getColorValue().intValue())));
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
      prevPage.setName(Text.translatable("gui.borislib.prev_page_title", this.page, this.numPages).withColor(Formatting.AQUA.getColorValue().intValue()));
      prevPage.addLoreLine(Text.translatable("text.borislib.two_elements", Text.translatable("gui.borislib.click").withColor(Formatting.GREEN.getColorValue().intValue()), Text.translatable("gui.borislib.prev_page_sub").withColor(Formatting.DARK_AQUA.getColorValue().intValue())));
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
         archetypes.add(ArchetypeRegistry.ARCHETYPES.get(i));
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
