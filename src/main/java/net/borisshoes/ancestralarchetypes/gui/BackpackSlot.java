package net.borisshoes.ancestralarchetypes.gui;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BackpackSlot extends Slot {
   
   public BackpackSlot(Container inventory, int index, int x, int y){
      super(inventory, index, x, y);
   }
   
   @Override
   public boolean mayPlace(ItemStack stack){
      return isValidItem(stack);
   }
   
   public static boolean isValidItem(ItemStack stack){
      return !stack.is(ArchetypeRegistry.BACKPACK_DISALLOWED_ITEMS);
   }
}
