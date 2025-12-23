package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class EntityUseCallback {
   public static InteractionResult useEntity(Player playerEntity, Level world, InteractionHand hand, Entity entity, EntityHitResult entityHitResult){
      InteractionResult mountCheck = mountCheck(playerEntity,entity);
      if(mountCheck != InteractionResult.PASS && mountCheck != InteractionResult.TRY_WITH_EMPTY_HAND){
         return mountCheck;
      }
      
      InteractionResult rideableCheck = rideableCheck(playerEntity,entity);
      if(rideableCheck != InteractionResult.PASS && mountCheck != InteractionResult.TRY_WITH_EMPTY_HAND){
         return rideableCheck;
      }
      
      return InteractionResult.PASS;
   }
   
   private static InteractionResult rideableCheck(Player playerEntity, Entity entity){
      if(!(playerEntity instanceof ServerPlayer user && entity instanceof ServerPlayer ridden)) return InteractionResult.PASS;
      if(!AncestralArchetypes.profile(ridden).hasAbility(ArchetypeRegistry.RIDEABLE)) return InteractionResult.PASS;
      
      int passengers = user.countPlayerPassengers();
      if(passengers > 0){
         return InteractionResult.FAIL;
      }
      if(CONFIG.getBoolean(ArchetypeRegistry.RIDEABLE_TEAM_ONLY)){
         PlayerTeam riddenTeam = ridden.getTeam();
         if(riddenTeam != null){
            if(user.getTeam() == null || !ridden.getTeam().getName().equals(user.getTeam().getName())) return InteractionResult.FAIL;
         }
      }
      user.startRiding(ridden);
      return InteractionResult.SUCCESS;
   }
   
   private static InteractionResult mountCheck(Player playerEntity, Entity entity){
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      boolean spiritMount = !tags.isEmpty();
      if(!spiritMount) return InteractionResult.PASS;
      
      LivingEntity rider = null;
      if(entity instanceof Camel camel){
         rider = camel.getControllingPassenger();
      }
      
      if((!(entity instanceof OwnableEntity tameable) || !(tameable.getOwner() instanceof ServerPlayer player))) return InteractionResult.FAIL;
      if(!player.equals(playerEntity) && (rider == null || !rider.equals(player))) return InteractionResult.FAIL;
      if(player.isShiftKeyDown()){
         if(tags.getFirst().contains(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT.id())){
            // Access inventory
            PlayerArchetypeData profile = profile(player);
            MountInventoryGui gui = new MountInventoryGui(player, profile.getMountInventory());
            gui.open();
            return InteractionResult.SUCCESS;
         }else{
            return InteractionResult.FAIL;
         }
      }
      return InteractionResult.PASS;
   }
}
