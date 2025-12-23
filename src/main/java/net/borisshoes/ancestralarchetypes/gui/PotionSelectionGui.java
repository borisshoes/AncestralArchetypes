package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.gui.GraphicalItem;
import net.borisshoes.borislib.gui.GuiHelper;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@SuppressWarnings("unchecked")
public class PotionSelectionGui extends SimpleGui {
   
   private static final ArrayList<PotionEntry> POTIONS = new ArrayList<>(Arrays.asList(
         new PotionEntry(new Tuple<>(Potions.FIRE_RESISTANCE, 0), new Tuple<>(Potions.LONG_FIRE_RESISTANCE, 2)),
         new PotionEntry(new Tuple<>(Potions.WATER_BREATHING,0), new Tuple<>(Potions.LONG_WATER_BREATHING,1)),
         new PotionEntry(new Tuple<>(Potions.NIGHT_VISION,0), new Tuple<>(Potions.LONG_NIGHT_VISION,1)),
         new PotionEntry(new Tuple<>(Potions.TURTLE_MASTER,1), new Tuple<>(Potions.LONG_TURTLE_MASTER,3)),
         new PotionEntry(new Tuple<>(Potions.SWIFTNESS,0), new Tuple<>(Potions.LONG_SWIFTNESS,2)),
         new PotionEntry(new Tuple<>(Potions.STRENGTH,0), new Tuple<>(Potions.LONG_STRENGTH,2)),
         new PotionEntry(new Tuple<>(Potions.LEAPING,0), new Tuple<>(Potions.LONG_LEAPING,1)),
         
         new PotionEntry(new Tuple<>(Potions.SLOW_FALLING,0), new Tuple<>(Potions.LONG_SLOW_FALLING,1)),
         new PotionEntry(new Tuple<>(Potions.REGENERATION,0), new Tuple<>(Potions.LONG_REGENERATION,2)),
         new PotionEntry(new Tuple<>(Potions.INVISIBILITY,0), new Tuple<>(Potions.LONG_INVISIBILITY,1)),
         new PotionEntry(new Tuple<>(Potions.STRONG_TURTLE_MASTER,4)),
         new PotionEntry(new Tuple<>(Potions.STRONG_SWIFTNESS,2)),
         new PotionEntry(new Tuple<>(Potions.STRONG_STRENGTH,2)),
         new PotionEntry(new Tuple<>(Potions.STRONG_LEAPING,1)),
         
         new PotionEntry(new Tuple<>(Potions.HEALING,0), new Tuple<>(Potions.STRONG_HEALING,3)),
         new PotionEntry(true, new Tuple<>(Potions.HEALING,2), new Tuple<>(Potions.STRONG_HEALING,4)),
         new PotionEntry(new Tuple<>(Potions.WATER,0)),
         new PotionEntry(new Tuple<>(Potions.SLOWNESS,0), new Tuple<>(Potions.LONG_SLOWNESS,2)),
         new PotionEntry(new Tuple<>(Potions.STRONG_SLOWNESS,2)),
         new PotionEntry(new Tuple<>(Potions.POISON,1), new Tuple<>(Potions.LONG_POISON,3)),
         new PotionEntry(new Tuple<>(Potions.STRONG_POISON,2)),
         
         new PotionEntry(new Tuple<>(Potions.HARMING,1), new Tuple<>(Potions.STRONG_HARMING,4)),
         new PotionEntry(true, new Tuple<>(Potions.HARMING,2), new Tuple<>(Potions.STRONG_HARMING,4)),
         new PotionEntry(new Tuple<>(Potions.WEAKNESS,0), new Tuple<>(Potions.LONG_WEAKNESS,2)),
         new PotionEntry(new Tuple<>(Potions.WIND_CHARGED,1)),
         new PotionEntry(new Tuple<>(Potions.WEAVING,1)),
         new PotionEntry(new Tuple<>(Potions.OOZING,2)),
         new PotionEntry(new Tuple<>(Potions.INFESTED,2))
   ));
   
   private final HashMap<Integer, Integer> map;
   private boolean splash;
   
   public PotionSelectionGui(ServerPlayer player){
      super(MenuType.GENERIC_9x6, player, false);
      setTitle(Component.translatable("text.ancestralarchetypes.potion_brewer_gui_title"));
      this.map = new HashMap<>();
      build();
   }
   
   @Override
   public boolean onAnyClick(int index, ClickType type, net.minecraft.world.inventory.ClickType action){
      if(this.map.containsKey(index)){
         PlayerArchetypeData profile = profile(player);
         long timeOfDay = player.level().getDayTime();
         int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
         int curPhase = day % 8;
         int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT_WITCH) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
         
         PotionEntry entry = POTIONS.get(this.map.get(index));
         Holder<Potion> potion = entry.getPotion(moonLevel);
         if(potion != null){
            profile.setPotionType(new Tuple<>(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION), potion));
            SoundUtils.playSongToPlayer(player, SoundEvents.BREWING_STAND_BREW,0.3f,1);
            close();
         }else{
            SoundUtils.playSongToPlayer(player, SoundEvents.NOTE_BLOCK_SNARE,1.0f,0.5f);
         }
      }else if(index == 8){
         this.splash = !this.splash;
         build();
      }
      return true;
   }
   
   public void build(){
      PlayerArchetypeData profile = profile(player);
      this.map.clear();
      GuiHelper.outlineGUI(this,0x6316c4, Component.empty());
      
      GuiElementBuilder potionTypeItem = GuiElementBuilder.from(PotionContents.createItemStack(splash ? Items.SPLASH_POTION : Items.POTION, Potions.HEALING)).hideDefaultTooltip();
      potionTypeItem.setName(Component.translatable(splash ? "item.minecraft.splash_potion" : "item.minecraft.potion").withStyle(splash ? ChatFormatting.AQUA : ChatFormatting.GREEN));
      potionTypeItem.addLoreLineRaw(Component.translatable("text.ancestralarchetypes.potion_brewer_gui_toggle"));
      setSlot(8,potionTypeItem);
      
      long timeOfDay = player.level().getDayTime();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT_WITCH) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
      
      for(int i = 0; i < POTIONS.size(); i++){
         int row = i / 7;
         int slot = 10 + 9*row + (i%7);
         
         PotionEntry entry = POTIONS.get(i);
         Holder<Potion> potion = entry.getPotion(moonLevel);
         GuiElementBuilder potionItem;
         if(potion != null){
            potionItem = GuiElementBuilder.from(PotionContents.createItemStack(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION),potion));
            potionItem.addLoreLineRaw(Component.literal(""));
            potionItem.addLoreLineRaw(Component.translatable("text.ancestralarchetypes.potion_brewer_gui_select").withStyle(ChatFormatting.LIGHT_PURPLE));
            if(entry.getUpgradePhase(moonLevel) != 5){
               potionItem.addLoreLineRaw(Component.translatable("text.ancestralarchetypes.potion_brewer_gui_upgrades").append(getTranslationForMoonPhase(entry.getUpgradePhase(moonLevel)).withStyle(ChatFormatting.WHITE)));
            }
         }else{
            potionItem = GuiElementBuilder.from(GraphicalItem.with(entry.lingering() ? ArchetypeRegistry.LOCKED_LINGERING_POTION : (splash ? ArchetypeRegistry.LOCKED_SPLASH_POTION : ArchetypeRegistry.LOCKED_POTION))).hideDefaultTooltip();
            potionItem.setName(PotionContents.createItemStack(entry.lingering() ? Items.LINGERING_POTION : (splash ? Items.SPLASH_POTION : Items.POTION),entry.getPotion(entry.getMinPhase())).getHoverName());
            potionItem.addLoreLineRaw(Component.translatable("text.ancestralarchetypes.potion_brewer_gui_locked").append(getTranslationForMoonPhase(entry.getMinPhase()).withStyle(ChatFormatting.WHITE)));
         }
         
         setSlot(slot,potionItem);
         
         this.map.put(slot,i);
      }
   }
   
   public static boolean isUnlocked(ServerPlayer player, Holder<Potion> potion){
      if(potion == null) return false;
      PlayerArchetypeData profile = profile(player);
      long timeOfDay = player.level().getDayTime();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT_WITCH) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
      
      for(PotionEntry entry : POTIONS){
         for(Tuple<Holder<Potion>, Integer> pair : entry.potionLevels){
            if(pair.getA().equals(potion) && moonLevel >= pair.getB()) return true;
         }
      }
      return false;
   }
   
   private MutableComponent getTranslationForMoonPhase(int moonPhase){
      return switch(moonPhase){
         case 0 -> Component.translatable("text.ancestralarchetypes.new_moon");
         case 1 -> Component.translatable("text.ancestralarchetypes.crescent_moon");
         case 2 -> Component.translatable("text.ancestralarchetypes.quarter_moon");
         case 3 -> Component.translatable("text.ancestralarchetypes.gibbous_moon");
         case 4 -> Component.translatable("text.ancestralarchetypes.full_moon");
         default -> throw new IllegalStateException("Unexpected value: " + moonPhase);
      };
   }
   
   private record PotionEntry(boolean lingering, Tuple<Holder<Potion>, Integer>... potionLevels){
      private PotionEntry(Tuple<Holder<Potion>, Integer>... potionLevels){
         this(false, potionLevels);
      }
      
      public Holder<Potion> getPotion(int moonLevel){
         int maxPhase = -1;
         Holder<Potion> potion = null;
         for(Tuple<Holder<Potion>, Integer> pair : potionLevels){
            if(moonLevel >= pair.getB() && pair.getB() > maxPhase){
               maxPhase = pair.getB();
               potion = pair.getA();
            }
         }
         return potion;
      }
      
      public int getMinPhase(){
         int minPhase = 5;
         for(Tuple<Holder<Potion>, Integer> pair : potionLevels){
            if(pair.getB() < minPhase) minPhase = pair.getB();
         }
         return minPhase;
      }
      
      public int getUpgradePhase(int moonLevel){
         int minPhase = 5;
         for(Tuple<Holder<Potion>, Integer> pair : potionLevels){
            if(moonLevel < pair.getB() && pair.getB() < minPhase){
               minPhase = pair.getB();
            }
         }
         return minPhase;
      }
   }
}
