package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.timers.TickTimerCallback;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class WaxShieldCallback extends TickTimerCallback {
   
   private final float hearts;
   
   public WaxShieldCallback(int time, ServerPlayer player, float hearts){
      super(time, null, player);
      this.hearts = hearts;
   }
   
   public float getHearts(){
      return hearts;
   }
   
   @Override
   public void onTimer(){
      try{
         ServerPlayer player1 = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
         if(player1 == null){
            BorisLib.addLoginCallback(new WaxShieldLoginCallback(player,hearts));
         }else{
            float removed = Math.max(0,player1.getAbsorptionAmount()-hearts);
            if(player1.getAbsorptionAmount() != 0){
               SoundUtils.playSongToPlayer(player1, SoundEvents.HONEYCOMB_WAX_ON, 1.0f, .3f);
            }
            MinecraftUtils.removeMaxAbsorption(player1, Identifier.fromNamespaceAndPath(MOD_ID, ArchetypeRegistry.WAX_SHIELD.id()),hearts);
            player1.setAbsorptionAmount(removed);
         }
      }catch(Exception e){
         e.printStackTrace();
      }
   }
}
