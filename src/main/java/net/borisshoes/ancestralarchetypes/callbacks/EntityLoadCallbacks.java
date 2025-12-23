package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class EntityLoadCallbacks {
   public static void unloadEntity(Entity entity, ServerLevel serverWorld){
      List<String> tags = entity.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      boolean spiritMount = !tags.isEmpty();
      
      if(spiritMount && entity.getRemovalReason() == Entity.RemovalReason.KILLED){
         if(entity instanceof OwnableEntity tameable && tameable.getOwner() instanceof ServerPlayer player){
            PlayerArchetypeData profile = profile(player);
            ArchetypeAbility ability = AncestralArchetypes.abilityFromTag(tags.getFirst());
            if(ability != null){
               profile.setAbilityCooldown(ability, CONFIG.getInt(ArchetypeRegistry.SPIRIT_MOUNT_KILL_COOLDOWN));
               player.displayClientMessage(Component.translatable("text.ancestralarchetypes.spirit_mount_unsummon").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
            }
         }
      }
   }
}
