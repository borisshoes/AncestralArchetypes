package net.borisshoes.ancestralarchetypes.gui;

import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public class BackpackGui extends SimpleGui {
   
   public BackpackGui(ServerPlayerEntity player, Inventory inv){
      super(ScreenHandlerType.GENERIC_9X2, player, false);
      setTitle(Text.translatable("text.ancestralarchetypes.backpack_inventory"));
      
      for(int i = 0; i < this.size; i++){
         setSlotRedirect(i,new BackpackSlot(inv,i,i%9,i/9));
      }
      
      SoundUtils.playSongToPlayer(player, SoundEvents.ITEM_BUNDLE_DROP_CONTENTS,1.5f,0.75f);
   }
}
