package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.borislib.events.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypesId;

public class EcholocationEntityGlowEvent extends Event {
   
   final Entity entity;
   final ServerPlayer player;
   
   public EcholocationEntityGlowEvent(Entity entity, ServerPlayer player){
      super(archetypesId("echolocation_glow"), 60);
      this.entity = entity;
      this.player = player;
   }
   
   @Override
   public void onExpiry(){
      super.onExpiry();
      List<? extends EcholocationEntityGlowEvent> events = Event.getEventsOfType(this.getClass());
      boolean found = false;
      for(EcholocationEntityGlowEvent event : events){
         if(event == this) continue;
         if(!event.player.getUUID().equals(this.player.getUUID())) continue;
         if(!event.entity.getUUID().equals(this.entity.getUUID())) continue;
         if(event.timeAlive < event.lifespan){
            found = true;
            break;
         }
      }
      if(!found){
         ArchetypeUtils.removeGlow(player, entity);
      }
   }
}
