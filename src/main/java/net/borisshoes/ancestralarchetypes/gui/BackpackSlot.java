package net.borisshoes.ancestralarchetypes.gui;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class BackpackSlot extends Slot {
   
   public BackpackSlot(Inventory inventory, int index, int x, int y){
      super(inventory, index, x, y);
   }
   
   @Override
   public boolean canInsert(ItemStack stack){
      return isValidItem(stack);
   }
   
   public static boolean isValidItem(ItemStack stack){
      return !stack.isIn(ArchetypeRegistry.BACKPACK_DISALLOWED_ITEMS);
   }
}
