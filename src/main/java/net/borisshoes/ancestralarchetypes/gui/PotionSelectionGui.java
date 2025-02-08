package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.GraphicalItem;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@SuppressWarnings("unchecked")
public class PotionSelectionGui extends SimpleGui {
   
   private static final ArrayList<PotionEntry> POTIONS = new ArrayList<>(Arrays.asList(
         new PotionEntry(new Pair<>(Potions.FIRE_RESISTANCE, 0), new Pair<>(Potions.LONG_FIRE_RESISTANCE, 2)),
         new PotionEntry(new Pair<>(Potions.WATER_BREATHING,0), new Pair<>(Potions.LONG_WATER_BREATHING,1)),
         new PotionEntry(new Pair<>(Potions.NIGHT_VISION,0), new Pair<>(Potions.LONG_NIGHT_VISION,1)),
         new PotionEntry(new Pair<>(Potions.TURTLE_MASTER,1), new Pair<>(Potions.LONG_TURTLE_MASTER,3)),
         new PotionEntry(new Pair<>(Potions.SWIFTNESS,0), new Pair<>(Potions.LONG_SWIFTNESS,2)),
         new PotionEntry(new Pair<>(Potions.STRENGTH,0), new Pair<>(Potions.LONG_STRENGTH,2)),
         new PotionEntry(new Pair<>(Potions.LEAPING,0), new Pair<>(Potions.LONG_LEAPING,1)),
         
         new PotionEntry(new Pair<>(Potions.SLOW_FALLING,0), new Pair<>(Potions.LONG_SLOW_FALLING,1)),
         new PotionEntry(new Pair<>(Potions.REGENERATION,0), new Pair<>(Potions.LONG_REGENERATION,2)),
         new PotionEntry(new Pair<>(Potions.INVISIBILITY,0), new Pair<>(Potions.LONG_INVISIBILITY,1)),
         new PotionEntry(new Pair<>(Potions.STRONG_TURTLE_MASTER,4)),
         new PotionEntry(new Pair<>(Potions.STRONG_SWIFTNESS,2)),
         new PotionEntry(new Pair<>(Potions.STRONG_STRENGTH,2)),
         new PotionEntry(new Pair<>(Potions.STRONG_LEAPING,1)),
         
         new PotionEntry(new Pair<>(Potions.HEALING,0), new Pair<>(Potions.STRONG_HEALING,3)),
         new PotionEntry(true, new Pair<>(Potions.HEALING,2), new Pair<>(Potions.STRONG_HEALING,4)),
         new PotionEntry(new Pair<>(Potions.WATER,0)),
         new PotionEntry(new Pair<>(Potions.SLOWNESS,0), new Pair<>(Potions.LONG_SLOWNESS,2)),
         new PotionEntry(new Pair<>(Potions.STRONG_SLOWNESS,2)),
         new PotionEntry(new Pair<>(Potions.POISON,1), new Pair<>(Potions.LONG_POISON,3)),
         new PotionEntry(new Pair<>(Potions.STRONG_POISON,2)),
         
         new PotionEntry(new Pair<>(Potions.HARMING,1), new Pair<>(Potions.STRONG_HARMING,4)),
         new PotionEntry(true, new Pair<>(Potions.HARMING,2), new Pair<>(Potions.STRONG_HARMING,4)),
         new PotionEntry(new Pair<>(Potions.WEAKNESS,0), new Pair<>(Potions.LONG_WEAKNESS,2)),
         new PotionEntry(new Pair<>(Potions.WIND_CHARGED,1)),
         new PotionEntry(new Pair<>(Potions.WEAVING,1)),
         new PotionEntry(new Pair<>(Potions.OOZING,2)),
         new PotionEntry(new Pair<>(Potions.INFESTED,2))
   ));
   
   private final HashMap<Integer, Integer> map;
   private boolean splash;
   
   public PotionSelectionGui(ServerPlayerEntity player){
      super(ScreenHandlerType.GENERIC_9X6, player, false);
      setTitle(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_title"));
      this.map = new HashMap<>();
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, SlotActionType action){
      if(this.map.containsKey(index)){
         IArchetypeProfile profile = profile(player);
         long timeOfDay = player.getServerWorld().getTimeOfDay();
         int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
         int curPhase = day % 8;
         int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
         
         PotionEntry entry = POTIONS.get(this.map.get(index));
         RegistryEntry<Potion> potion = entry.getPotion(moonLevel);
         if(potion != null){
            profile.setPotionType(new Pair<>(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION), potion));
            SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_BREWING_STAND_BREW,0.3f,1);
            close();
         }else{
            SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_NOTE_BLOCK_SNARE,1.0f,0.5f);
         }
      }else if(index == 8){
         this.splash = !this.splash;
         build();
      }
      return true;
   }
   
   public void build(){
      IArchetypeProfile profile = profile(player);
      this.map.clear();
      MiscUtils.outlineGUI(this,0x6316c4, Text.empty());
      
      GuiElementBuilder potionTypeItem = GuiElementBuilder.from(PotionContentsComponent.createStack(splash ? Items.SPLASH_POTION : Items.POTION,Potions.HEALING)).hideDefaultTooltip();
      potionTypeItem.setName(Text.translatable(splash ? "item.minecraft.splash_potion" : "item.minecraft.potion").formatted(splash ? Formatting.AQUA : Formatting.GREEN));
      potionTypeItem.addLoreLineRaw(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_toggle"));
      setSlot(8,potionTypeItem);
      
      long timeOfDay = player.getServerWorld().getTimeOfDay();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
      
      for(int i = 0; i < POTIONS.size(); i++){
         int row = i / 7;
         int slot = 10 + 9*row + (i%7);
         
         PotionEntry entry = POTIONS.get(i);
         RegistryEntry<Potion> potion = entry.getPotion(moonLevel);
         GuiElementBuilder potionItem;
         if(potion != null){
            potionItem = GuiElementBuilder.from(PotionContentsComponent.createStack(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION),potion));
            potionItem.addLoreLineRaw(Text.literal(""));
            potionItem.addLoreLineRaw(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_select").formatted(Formatting.LIGHT_PURPLE));
            if(entry.getUpgradePhase(moonLevel) != 5){
               potionItem.addLoreLineRaw(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_upgrades").append(getTranslationForMoonPhase(entry.getUpgradePhase(moonLevel)).formatted(Formatting.WHITE)));
            }
         }else{
            potionItem = GuiElementBuilder.from(GraphicalItem.with(entry.lingering() ? GraphicalItem.GraphicItems.LOCKED_LINGERING_POTION : (splash ? GraphicalItem.GraphicItems.LOCKED_SPLASH_POTION : GraphicalItem.GraphicItems.LOCKED_POTION))).hideDefaultTooltip();
            potionItem.setName(PotionContentsComponent.createStack(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION),entry.getPotion(entry.getMinPhase())).getName());
            potionItem.addLoreLineRaw(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_locked").append(getTranslationForMoonPhase(entry.getMinPhase()).formatted(Formatting.WHITE)));
         }
         
         setSlot(slot,potionItem);
         
         this.map.put(slot,i);
      }
   }
   
   public static boolean isUnlocked(ServerPlayerEntity player, RegistryEntry<Potion> potion){
      if(potion == null) return false;
      IArchetypeProfile profile = profile(player);
      long timeOfDay = player.getServerWorld().getTimeOfDay();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
      
      for(PotionEntry entry : POTIONS){
         for(Pair<RegistryEntry<Potion>, Integer> pair : entry.potionLevels){
            if(pair.getLeft().equals(potion) && moonLevel >= pair.getRight()) return true;
         }
      }
      return false;
   }
   
   private MutableText getTranslationForMoonPhase(int moonPhase){
      return switch(moonPhase){
         case 0 -> Text.translatable("text.ancestralarchetypes.new_moon");
         case 1 -> Text.translatable("text.ancestralarchetypes.crescent_moon");
         case 2 -> Text.translatable("text.ancestralarchetypes.quarter_moon");
         case 3 -> Text.translatable("text.ancestralarchetypes.gibbous_moon");
         case 4 -> Text.translatable("text.ancestralarchetypes.full_moon");
         default -> throw new IllegalStateException("Unexpected value: " + moonPhase);
      };
   }
   
   private record PotionEntry(boolean lingering, Pair<RegistryEntry<Potion>, Integer>... potionLevels){
      private PotionEntry(Pair<RegistryEntry<Potion>, Integer>... potionLevels){
         this(false, potionLevels);
      }
      
      public RegistryEntry<Potion> getPotion(int moonLevel){
         int maxPhase = -1;
         RegistryEntry<Potion> potion = null;
         for(Pair<RegistryEntry<Potion>, Integer> pair : potionLevels){
            if(moonLevel >= pair.getRight() && pair.getRight() > maxPhase){
               maxPhase = pair.getRight();
               potion = pair.getLeft();
            }
         }
         return potion;
      }
      
      public int getMinPhase(){
         int minPhase = 5;
         for(Pair<RegistryEntry<Potion>, Integer> pair : potionLevels){
            if(pair.getRight() < minPhase) minPhase = pair.getRight();
         }
         return minPhase;
      }
      
      public int getUpgradePhase(int moonLevel){
         int minPhase = 5;
         for(Pair<RegistryEntry<Potion>, Integer> pair : potionLevels){
            if(moonLevel < pair.getRight() && pair.getRight() < minPhase){
               minPhase = pair.getRight();
            }
         }
         return minPhase;
      }
   }
}
