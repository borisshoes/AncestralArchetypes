package net.borisshoes.ancestralarchetypes.misc;

import net.minecraft.server.network.ServerPlayerEntity;

public class SpyglassRevealEvent {
   private final ServerPlayerEntity inspector;
   private final ServerPlayerEntity target;
   private final boolean reset;
   private int cooldown;
   
   public SpyglassRevealEvent(ServerPlayerEntity inspector, ServerPlayerEntity target, int cooldown, boolean reset){
      this.inspector = inspector;
      this.target = target;
      this.cooldown = cooldown;
      this.reset = reset;
   }
   
   public void tickCooldown(){
      this.cooldown--;
   }
   
   public ServerPlayerEntity getInspector(){
      return inspector;
   }
   
   public ServerPlayerEntity getTarget(){
      return target;
   }
   
   public int getCooldown(){
      return cooldown;
   }
   
   public boolean isReset(){
      return reset;
   }
}
