package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(AbstractThrownPotion.class)
public class AbstractThrownPotionMixin {

   @Inject(method = "onHitAsWater", at = @At("HEAD"))
   private void archetypes$waterDamage(ServerLevel world, CallbackInfo ci){
      AbstractThrownPotion entity = (AbstractThrownPotion) (Object) this;
      AABB box = entity.getBoundingBox().inflate(4.0, 2.0, 4.0);
      for (ServerPlayer playerEntity : world.getEntitiesOfClass(ServerPlayer.class, box, (e) -> true)){
         double d = entity.distanceToSqr(playerEntity);
         if(d < 16.0){
            PlayerArchetypeData profile = profile(playerEntity);
            if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && !playerEntity.hasEffect(MobEffects.WATER_BREATHING)){
               playerEntity.hurtServer(playerEntity.level(), world.damageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.GENERIC_BURN, playerEntity.getSoundSource(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
            }
         }
      }
   }
}
