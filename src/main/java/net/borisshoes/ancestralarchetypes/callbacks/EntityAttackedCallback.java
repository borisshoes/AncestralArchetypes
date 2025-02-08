package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EntityAttackedCallback {
   public static ActionResult attackedEntity(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      if(playerEntity instanceof ServerPlayerEntity player && entity instanceof LivingEntity livingEntity){
         IArchetypeProfile profile = profile(player);
         
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && livingEntity.isAlive()){
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, ArchetypeConfig.getInt(ArchetypeRegistry.WITHERING_EFFECT_DURATION), 0, false, true, true), player);
         }
      }
      return ActionResult.PASS;
   }
}
