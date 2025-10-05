package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.arcananovum.callbacks.ShieldLoginCallback;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.timers.TickTimerCallback;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class WaxShieldCallback extends TickTimerCallback {
   
   private final float hearts;
   
   public WaxShieldCallback(int time, ServerPlayerEntity player, float hearts){
      super(time, null, player);
      this.hearts = hearts;
   }
   
   public float getHearts(){
      return hearts;
   }
   
   @Override
   public void onTimer(){
      try{
         ServerPlayerEntity player1 = player.getServer().getPlayerManager().getPlayer(player.getUuid());
         if(player1 == null){
            BorisLib.addLoginCallback(new WaxShieldLoginCallback(player,hearts));
         }else{
            float removed = Math.max(0,player1.getAbsorptionAmount()-hearts);
            if(player1.getAbsorptionAmount() != 0){
               SoundUtils.playSongToPlayer(player1, SoundEvents.ITEM_HONEYCOMB_WAX_ON, 1.0f, .3f);
            }
            MinecraftUtils.removeMaxAbsorption(player1,Identifier.of(MOD_ID, ArchetypeRegistry.WAX_SHIELD.getId()),hearts);
            player1.setAbsorptionAmount(removed);
         }
      }catch(Exception e){
         e.printStackTrace();
      }
   }
}
