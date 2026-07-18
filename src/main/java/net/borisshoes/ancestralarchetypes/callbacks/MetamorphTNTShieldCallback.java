package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.timers.TickTimerCallback;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.server.level.ServerPlayer;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypesId;

public class MetamorphTNTShieldCallback extends TickTimerCallback {
   
   private final float hearts;
   
   public MetamorphTNTShieldCallback(int time, ServerPlayer player, float hearts){
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
            BorisLib.addLoginCallback(new MetamorphTNTShieldLoginCallback(player, hearts));
         }else{
            float removed = Math.max(0, player1.getAbsorptionAmount() - hearts);
            MinecraftUtils.removeMaxAbsorption(player1, archetypesId(ArchetypeRegistry.METAMORPH.id()), hearts);
            player1.setAbsorptionAmount(removed);
         }
      }catch(Exception e){
         e.printStackTrace();
      }
   }
}
