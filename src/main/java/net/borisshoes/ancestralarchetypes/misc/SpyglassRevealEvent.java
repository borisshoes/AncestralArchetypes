package net.borisshoes.ancestralarchetypes.misc;

import net.minecraft.server.level.ServerPlayer;

public class SpyglassRevealEvent {
   private final ServerPlayer inspector;
   private final ServerPlayer target;
   private final boolean reset;
   private int cooldown;
   
   public SpyglassRevealEvent(ServerPlayer inspector, ServerPlayer target, int cooldown, boolean reset){
      this.inspector = inspector;
      this.target = target;
      this.cooldown = cooldown;
      this.reset = reset;
   }
   
   public void tickCooldown(){
      this.cooldown--;
   }
   
   public ServerPlayer getInspector(){
      return inspector;
   }
   
   public ServerPlayer getTarget(){
      return target;
   }
   
   public int getCooldown(){
      return cooldown;
   }
   
   public boolean isReset(){
      return reset;
   }
}
