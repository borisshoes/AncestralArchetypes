package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.events.BulletTargetEvent;
import net.borisshoes.ancestralarchetypes.items.LevitationBulletItem;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.events.Event;
import net.borisshoes.borislib.timers.TickTimerCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class DeglowTimerCallback extends TickTimerCallback {
   public final ServerPlayer player;
   public final LivingEntity target;
   
   public DeglowTimerCallback(ServerPlayer player, LivingEntity target) {
      super(1, null, null);
      this.player = player;
      this.target = target;
   }
   
   public void onTimer() {
      boolean found = Event.getEventsOfType(BulletTargetEvent.class).stream().anyMatch(e -> e.player.getId() == player.getId() && e.target.getId() == target.getId());
      if(found){
         BorisLib.addTickTimerCallback(new DeglowTimerCallback(player, target));
      }else{
         LevitationBulletItem.removeGlow(player,target);
      }
   }
}