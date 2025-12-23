package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EntityAttackedCallback {
   public static InteractionResult attackedEntity(Player playerEntity, Level world, InteractionHand hand, Entity entity, EntityHitResult entityHitResult){
      if(playerEntity instanceof ServerPlayer player && entity instanceof LivingEntity livingEntity){
         PlayerArchetypeData profile = profile(player);
         
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && livingEntity.isAlive()){
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, CONFIG.getInt(ArchetypeRegistry.WITHERING_EFFECT_DURATION), 0, false, true, true), player);
         }
         if(profile.hasAbility(ArchetypeRegistry.BLAZING_STRIKE) && livingEntity.isAlive() && !livingEntity.fireImmune()){
            livingEntity.igniteForTicks(CONFIG.getInt(ArchetypeRegistry.BLAZING_STRIKE_DURATION));
         }
         if(profile.hasAbility(ArchetypeRegistry.VENOMOUS) && livingEntity.isAlive()){
            int duration = CONFIG.getInt(ArchetypeRegistry.VENOMOUS_POISON_DURATION);
            int amplifier = CONFIG.getInt(ArchetypeRegistry.VENOMOUS_POISON_STRENGTH) - 1;
            if(profile.hasAbility(ArchetypeRegistry.MOONLIT_CAVE_SPIDER)){
               int durPerPhase = CONFIG.getInt(ArchetypeRegistry.MOONLIT_CAVE_SPIDER_VENOM_DURATION_PER_PHASE);
               double ampPerPhase = CONFIG.getDouble(ArchetypeRegistry.MOONLIT_CAVE_SPIDER_VENOM_STRENGTH_PER_PHASE);
               long timeOfDay = player.level().getDayTime();
               int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
               int curPhase = day % 8;
               int moonLevel = profile.hasAbility(ArchetypeRegistry.MOONLIT_WITCH) ? Math.abs(-curPhase+4) : 4; // 0 - new moon, 4 - full moon
               duration += moonLevel*durPerPhase;
               amplifier += (int)(moonLevel*ampPerPhase);
            }
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier, false, true, true), player);
         }
      }
      return InteractionResult.PASS;
   }
}
