package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Tameable;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class EntityLoadCallbacks {
   public static void unloadEntity(Entity entity, ServerWorld serverWorld){
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      boolean spiritMount = !tags.isEmpty();
      
      if(spiritMount && entity.getRemovalReason() == Entity.RemovalReason.KILLED){
         if(entity instanceof Tameable tameable && tameable.getOwner() instanceof ServerPlayerEntity player){
            IArchetypeProfile profile = profile(player);
            ArchetypeAbility ability = AncestralArchetypes.abilityFromTag(tags.getFirst());
            if(ability != null){
               profile.setAbilityCooldown(ability, CONFIG.getInt(ArchetypeRegistry.SPIRIT_MOUNT_KILL_COOLDOWN));
               player.sendMessage(Text.translatable("text.ancestralarchetypes.spirit_mount_unsummon").formatted(Formatting.RED,Formatting.ITALIC),true);
            }
         }
      }
   }
}
