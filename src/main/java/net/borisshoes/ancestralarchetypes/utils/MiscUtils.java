package net.borisshoes.ancestralarchetypes.utils;

import com.google.common.collect.HashMultimap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.items.GraphicalItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class MiscUtils {
   
   public static void giveStacks(PlayerEntity player, ItemStack... stacks){
      returnItems(new SimpleInventory(stacks),player);
   }
   
   public static void returnItems(Inventory inv, PlayerEntity player){
      if(inv == null) return;
      for(int i=0; i<inv.size();i++){
         ItemStack stack = inv.getStack(i).copy();
         if(!stack.isEmpty()){
            inv.setStack(0,ItemStack.EMPTY);
            
            ItemEntity itemEntity;
            boolean bl = player.getInventory().insertStack(stack);
            if(!bl || !stack.isEmpty()){
               itemEntity = player.dropItem(stack, false);
               if(itemEntity == null) continue;
               itemEntity.resetPickupDelay();
               itemEntity.setOwner(player.getUuid());
               continue;
            }
            stack.setCount(1);
            itemEntity = player.dropItem(stack, false);
            if(itemEntity != null){
               itemEntity.setDespawnImmediately();
            }
            player.currentScreenHandler.sendContentUpdates();
         }
      }
   }
   
   public static UUID getUUIDOrNull(String str){
      try{
         return UUID.fromString(str);
      }catch(Exception e){
         return null;
      }
   }
   
   public static ArchetypeAbility abilityFromTag(String tag){
      int lastDotIndex = tag.lastIndexOf(".");
      if (lastDotIndex == -1) {
         return null;
      }
      return ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID,tag.substring(lastDotIndex + 1)));
   }
   
   private static final ArrayList<Pair<Formatting,Integer>> COLOR_MAP = new ArrayList<>(Arrays.asList(
         new Pair<>(Formatting.BLACK,0x000000),
         new Pair<>(Formatting.DARK_BLUE,0x0000AA),
         new Pair<>(Formatting.DARK_GREEN,0x00AA00),
         new Pair<>(Formatting.DARK_AQUA,0x00AAAA),
         new Pair<>(Formatting.DARK_RED,0xAA0000),
         new Pair<>(Formatting.DARK_PURPLE,0xAA00AA),
         new Pair<>(Formatting.GOLD,0xFFAA00),
         new Pair<>(Formatting.GRAY,0xAAAAAA),
         new Pair<>(Formatting.DARK_GRAY,0x555555),
         new Pair<>(Formatting.BLUE,0x5555FF),
         new Pair<>(Formatting.GREEN,0x55FF55),
         new Pair<>(Formatting.AQUA,0x55FFFF),
         new Pair<>(Formatting.RED,0xFF5555),
         new Pair<>(Formatting.LIGHT_PURPLE,0xFF55FF),
         new Pair<>(Formatting.YELLOW,0xFFFF55),
         new Pair<>(Formatting.WHITE,0xFFFFFF)
   ));
   
   public static Formatting getClosestFormatting(int colorRGB){
      Formatting closest = Formatting.WHITE;
      double cDist = Integer.MAX_VALUE;
      for(Pair<Formatting, Integer> pair : COLOR_MAP){
         int repColor = pair.getRight();
         double rDist = (((repColor>>16)&0xFF)-((colorRGB>>16)&0xFF))*0.30;
         double gDist = (((repColor>>8)&0xFF)-((colorRGB>>8)&0xFF))*0.59;
         double bDist = ((repColor&0xFF)-(colorRGB&0xFF))*0.11;
         double dist = rDist*rDist + gDist*gDist + bDist*bDist;
         if(dist < cDist){
            cDist = dist;
            closest = pair.getLeft();
         }
      }
      return closest;
   }
   
   public static MutableText withColor(MutableText text, int color){
      return text.setStyle(text.getStyle().withColor(color));
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText){
      outlineGUI(gui,color,borderText,null);
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText, List<Text> lore){
      for(int i = 0; i < gui.getSize(); i++){
         gui.clearSlot(i);
         GuiElementBuilder menuItem;
         boolean top = i/9 == 0;
         boolean bottom = i/9 == (gui.getSize()/9 - 1);
         boolean left = i%9 == 0;
         boolean right = i%9 == 8;
         
         if(top){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
            }
         }else if(bottom){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM,color));
            }
         }else if(left){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_LEFT,color));
         }else if(right){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_RIGHT,color));
         }else{
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
         }
         
         if(borderText.getString().isEmpty()){
            menuItem.hideTooltip();
         }else{
            menuItem.setName(borderText).hideDefaultTooltip();
            if(lore != null && !lore.isEmpty()){
               for(Text text : lore){
                  menuItem.addLoreLine(text);
               }
            }
         }
         
         gui.setSlot(i,menuItem);
      }
   }
   
   public static MutableText removeItalics(MutableText text){
      Style parentStyle = Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false).withBold(false).withUnderline(false).withObfuscated(false).withStrikethrough(false);
      return text.setStyle(text.getStyle().withParent(parentStyle));
   }
   
   public static RegistryEntry<Enchantment> getEnchantment(RegistryKey<Enchantment> key){
      if(AncestralArchetypes.SERVER == null){
         AncestralArchetypes.log(2,"Attempted to access Enchantment "+key.toString()+" before DRM is available");
         return null;
      }
      Optional<RegistryEntry.Reference<Enchantment>> opt = AncestralArchetypes.SERVER.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
      return opt.orElse(null);
   }
   
   
   public static void attributeEffect(LivingEntity livingEntity, RegistryEntry<EntityAttribute> attribute, double value, EntityAttributeModifier.Operation operation, Identifier identifier, boolean remove){
      boolean hasMod = livingEntity.getAttributes().hasModifierForAttribute(attribute,identifier);
      if(hasMod && remove){ // Remove the modifier
         HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = HashMultimap.create();
         map.put(attribute, new EntityAttributeModifier(identifier, value, operation));
         livingEntity.getAttributes().removeModifiers(map);
      }else if(!hasMod && !remove){ // Add the modifier
         HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = HashMultimap.create();
         map.put(attribute, new EntityAttributeModifier(identifier, value, operation));
         livingEntity.getAttributes().addTemporaryModifiers(map);
      }
   }
}
