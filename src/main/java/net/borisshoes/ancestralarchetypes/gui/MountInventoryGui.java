package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;

public class MountInventoryGui extends SimpleGui {
   
   public MountInventoryGui(ServerPlayer player, Container inv){
      super(MenuType.GENERIC_9x6, player, false);
      setTitle(Component.translatable("text.ancestralarchetypes.spirit_mount_inventory"));
      
      for(int i = 0; i < this.size; i++){
         setSlotRedirect(i,new MountInventorySlot(inv,i,i%9,i/9));
      }
   }
}
