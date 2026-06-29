package net.borisshoes.ancestralarchetypes.events;

import net.borisshoes.borislib.events.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypesId;

public class BulletTargetEvent extends Event {
   public final ServerPlayer player;
   public final LivingEntity target;
   
   public BulletTargetEvent(ServerPlayer player, LivingEntity target){
      super(archetypesId("bullet_target"),5);
      this.player = player;
      this.target = target;
   }
}
