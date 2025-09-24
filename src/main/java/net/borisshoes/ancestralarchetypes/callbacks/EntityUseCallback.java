package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class EntityUseCallback {
   public static ActionResult useEntity(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      ActionResult mountCheck = mountCheck(playerEntity,entity);
      if(mountCheck != ActionResult.PASS && mountCheck != ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION){
         return mountCheck;
      }
      
      ActionResult rideableCheck = rideableCheck(playerEntity,entity);
      if(rideableCheck != ActionResult.PASS && mountCheck != ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION){
         return rideableCheck;
      }
      
      return ActionResult.PASS;
   }
   
   private static ActionResult rideableCheck(PlayerEntity playerEntity, Entity entity){
      if(!(playerEntity instanceof ServerPlayerEntity user && entity instanceof ServerPlayerEntity ridden)) return ActionResult.PASS;
      if(!AncestralArchetypes.profile(ridden).hasAbility(ArchetypeRegistry.RIDEABLE)) return ActionResult.PASS;
      
      int passengers = user.getPlayerPassengers();
      if(passengers > 0){
         return ActionResult.FAIL;
      }
      if(CONFIG.getBoolean(ArchetypeRegistry.RIDEABLE_TEAM_ONLY)){
         Team riddenTeam = ridden.getScoreboardTeam();
         if(riddenTeam != null){
            if(user.getScoreboardTeam() == null || !ridden.getScoreboardTeam().getName().equals(user.getScoreboardTeam().getName())) return ActionResult.FAIL;
         }
      }
      user.startRiding(ridden);
      return ActionResult.SUCCESS;
   }
   
   private static ActionResult mountCheck(PlayerEntity playerEntity, Entity entity){
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      boolean spiritMount = !tags.isEmpty();
      if(!spiritMount) return ActionResult.PASS;
      
      LivingEntity rider = null;
      if(entity instanceof CamelEntity camel){
         rider = camel.getControllingPassenger();
      }
      
      if((!(entity instanceof Tameable tameable) || !(tameable.getOwner() instanceof ServerPlayerEntity player))) return ActionResult.FAIL;
      if(!player.equals(playerEntity) && (rider == null || !rider.equals(player))) return ActionResult.FAIL;
      if(player.isSneaking()){
         if(tags.getFirst().contains(ArchetypeRegistry.DONKEY_SPIRIT_MOUNT.getId())){
            // Access inventory
            IArchetypeProfile profile = profile(player);
            MountInventoryGui gui = new MountInventoryGui(player, profile.getMountInventory());
            gui.open();
            return ActionResult.SUCCESS;
         }else{
            return ActionResult.FAIL;
         }
      }
      return ActionResult.PASS;
   }
}
