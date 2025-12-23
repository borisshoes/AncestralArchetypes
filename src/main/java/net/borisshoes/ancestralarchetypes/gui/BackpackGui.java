package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;

public class BackpackGui extends SimpleGui {
   
   public BackpackGui(ServerPlayer player, Container inv){
      super(MenuType.GENERIC_9x2, player, false);
      setTitle(Component.translatable("text.ancestralarchetypes.backpack_inventory"));
      
      for(int i = 0; i < this.size; i++){
         setSlotRedirect(i,new BackpackSlot(inv,i,i%9,i/9));
      }
      
      SoundUtils.playSongToPlayer(player, SoundEvents.BUNDLE_DROP_CONTENTS,1.5f,0.75f);
   }
}
