package net.borisshoes.ancestralarchetypes.events;

import net.borisshoes.borislib.events.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class BulletTargetEvent extends Event {
   public final ServerPlayerEntity player;
   public final LivingEntity target;
   
   public BulletTargetEvent(ServerPlayerEntity player, LivingEntity target){
      super(Identifier.of(MOD_ID,"bullet_target"),5);
      this.player = player;
      this.target = target;
   }
}
