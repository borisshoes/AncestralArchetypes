package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(Snowball.class)
public class SnowballMixin {
   
   @Inject(method = "onHitEntity", at = @At("TAIL"))
   private void archetypes$snowballDamage(EntityHitResult entityHitResult, CallbackInfo ci){
      Snowball snowball = (Snowball)(Object) this;
      Entity entity = entityHitResult.getEntity();
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD)){
            entity.hurtServer(player.level(), snowball.damageSources().thrown(snowball, snowball.getOwner()), (float) CONFIG.getDouble(ArchetypeRegistry.SNOWBALL_DAMAGE));
         }
      }
   }
}
