package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(SnowballEntity.class)
public class SnowballEntityMixin {
   
   @Inject(method = "onEntityHit", at = @At("TAIL"))
   private void archetypes_snowballDamage(EntityHitResult entityHitResult, CallbackInfo ci){
      SnowballEntity snowball = (SnowballEntity)(Object) this;
      Entity entity = entityHitResult.getEntity();
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD)){
            entity.damage(player.getServerWorld(), snowball.getDamageSources().thrown(snowball, snowball.getOwner()), (float) ArchetypeConfig.getDouble(ArchetypeRegistry.SNOWBALL_DAMAGE));
         }
      }
   }
}
