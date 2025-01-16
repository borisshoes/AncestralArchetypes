package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
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
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class PotionSelectionGui extends SimpleGui {
   
   private static final ArrayList<Pair<Item, RegistryEntry<Potion>>> POTIONS = new ArrayList<>(Arrays.asList(
         new Pair<>(Items.POTION, Potions.LONG_WATER_BREATHING),
         new Pair<>(Items.POTION, Potions.LONG_NIGHT_VISION),
         new Pair<>(Items.POTION, Potions.LONG_INVISIBILITY),
         new Pair<>(Items.POTION, Potions.LONG_TURTLE_MASTER),
         new Pair<>(Items.POTION, Potions.LONG_SWIFTNESS),
         new Pair<>(Items.POTION, Potions.LONG_STRENGTH),
         new Pair<>(Items.POTION, Potions.LONG_LEAPING),
         new Pair<>(Items.POTION, Potions.LONG_SLOW_FALLING),
         new Pair<>(Items.POTION, Potions.STRONG_TURTLE_MASTER),
         new Pair<>(Items.POTION, Potions.LONG_REGENERATION),
         new Pair<>(Items.POTION, Potions.STRONG_HEALING),
         new Pair<>(Items.POTION, Potions.STRONG_SWIFTNESS),
         new Pair<>(Items.POTION, Potions.STRONG_STRENGTH),
         new Pair<>(Items.POTION, Potions.STRONG_LEAPING),
         // Break in GUI
         new Pair<>(Items.SPLASH_POTION, Potions.STRONG_SLOWNESS),
         new Pair<>(Items.SPLASH_POTION, Potions.LONG_SLOWNESS),
         new Pair<>(Items.SPLASH_POTION, Potions.LONG_WEAKNESS),
         new Pair<>(Items.SPLASH_POTION, Potions.STRONG_POISON),
         new Pair<>(Items.SPLASH_POTION, Potions.LONG_POISON),
         new Pair<>(Items.SPLASH_POTION, Potions.STRONG_HARMING),
         new Pair<>(Items.LINGERING_POTION, Potions.STRONG_HARMING)
   ));
   
   private final HashMap<Integer, Integer> map;
   
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
         profile.setPotionType(POTIONS.get(this.map.get(index)));
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_BREWING_STAND_BREW,0.3f,1);
         close();
      }
      return true;
   }
   
   public void build(){
      this.map.clear();
      MiscUtils.outlineGUI(this,0x6316c4, Text.empty());
      setSlot(27, GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_LEFT_CONNECTOR,0x6316c4)).hideTooltip());
      setSlot(35, GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_RIGHT_CONNECTOR,0x6316c4)).hideTooltip());
      
      for(int i = 0; i < POTIONS.size(); i++){
         int row = i / 7;
         if(row == 2) row++;
         int slot = 10 + 9*row + (i%7);
         
         GuiElementBuilder potionItem = GuiElementBuilder.from(PotionContentsComponent.createStack(POTIONS.get(i).getLeft(),POTIONS.get(i).getRight()));
         potionItem.addLoreLineRaw(Text.translatable("text.ancestralarchetypes.potion_brewer_gui_select"));
         setSlot(slot,potionItem);
         
         this.map.put(slot,i);
      }
   }
}
