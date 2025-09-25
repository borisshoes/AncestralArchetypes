package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class MountInventoryGui extends SimpleGui {
   
   public MountInventoryGui(ServerPlayerEntity player, Inventory inv){
      super(ScreenHandlerType.GENERIC_9X6, player, false);
      setTitle(Text.translatable("text.ancestralarchetypes.spirit_mount_inventory"));
      
      for(int i = 0; i < this.size; i++){
         setSlotRedirect(i,new MountInventorySlot(inv,i,i%9,i/9));
      }
   }
}
