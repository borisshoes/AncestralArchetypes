package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PotionEntity.class)
public class PotionEntityMixin {

   @Inject(method = "explodeWaterPotion", at = @At("HEAD"))
   private void archetypes$waterDamage(ServerWorld world, CallbackInfo ci){
      PotionEntity entity = (PotionEntity) (Object) this;
      Box box = entity.getBoundingBox().expand(4.0, 2.0, 4.0);
      for (ServerPlayerEntity playerEntity : world.getEntitiesByClass(ServerPlayerEntity.class, box, (e) -> true)){
         double d = entity.squaredDistanceTo(playerEntity);
         if(d < 16.0){
            IArchetypeProfile profile = profile(playerEntity);
            if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER)){
               playerEntity.damage(playerEntity.getWorld(), world.getDamageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_GENERIC_BURN, playerEntity.getSoundCategory(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
            }
         }
      }
   }
}
