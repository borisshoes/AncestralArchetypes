package net.borisshoes.ancestralarchetypes.gui;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class MountInventorySlot extends Slot {
   
   public MountInventorySlot(Inventory inventory, int index, int x, int y){
      super(inventory, index, x, y);
   }
   
   @Override
   public boolean canInsert(ItemStack stack){
      return isValidItem(stack);
   }
   
   public static boolean isValidItem(ItemStack stack){
      return !stack.isIn(ArchetypeRegistry.ABILITY_ITEMS);
   }
}