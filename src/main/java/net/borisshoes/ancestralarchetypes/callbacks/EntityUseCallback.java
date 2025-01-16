package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.gui.MountInventoryGui;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EntityUseCallback {
   public static ActionResult useEntity(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      boolean spiritMount = !tags.isEmpty();
      if(!spiritMount) return ActionResult.PASS;
      
      if(entity instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity player){
         if(!player.equals(playerEntity)) return ActionResult.FAIL;
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
      }
      
      return ActionResult.PASS;
   }
}
