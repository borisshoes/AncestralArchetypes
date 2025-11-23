package net.borisshoes.ancestralarchetypes.callbacks;

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

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EntityAttackedCallback {
   public static ActionResult attackedEntity(PlayerEntity playerEntity, World world, Hand hand, Entity entity, EntityHitResult entityHitResult){
      if(playerEntity instanceof ServerPlayerEntity player && entity instanceof LivingEntity livingEntity){
         IArchetypeProfile profile = profile(player);
         
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && livingEntity.isAlive()){
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, CONFIG.getInt(ArchetypeRegistry.WITHERING_EFFECT_DURATION), 0, false, true, true), player);
         }
         if(profile.hasAbility(ArchetypeRegistry.BLAZING_STRIKE) && livingEntity.isAlive() && !livingEntity.isFireImmune()){
            livingEntity.setOnFireForTicks(CONFIG.getInt(ArchetypeRegistry.BLAZING_STRIKE_DURATION));
         }
         if(profile.hasAbility(ArchetypeRegistry.VENOMOUS) && livingEntity.isAlive()){
            int duration = CONFIG.getInt(ArchetypeRegistry.VENOMOUS_POISON_DURATION);
            int amplifier = CONFIG.getInt(ArchetypeRegistry.VENOMOUS_POISON_STRENGTH) - 1;
            if(profile.hasAbility(ArchetypeRegistry.MOONLIT_CAVE_SPIDER)){
               int durPerPhase = CONFIG.getInt(ArchetypeRegistry.MOONLIT_CAVE_SPIDER_VENOM_DURATION_PER_PHASE);
               double ampPerPhase = CONFIG.getDouble(ArchetypeRegistry.MOONLIT_CAVE_SPIDER_VENOM_STRENGTH_PER_PHASE);
               long timeOfDay = player.getEntityWorld().getTimeOfDay();
               int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
               int curPhase = day % 8;
               int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT_WITCH) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
               duration += moonLevel*durPerPhase;
               amplifier += (int)(moonLevel*ampPerPhase);
            }
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, duration, amplifier, false, true, true), player);
         }
      }
      return ActionResult.PASS;
   }
}
