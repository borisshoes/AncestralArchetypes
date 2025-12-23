package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(LayeredCauldronBlock.class)
public class LayeredCauldronBlockMixin {
   
   @Inject(method = "entityInside", at = @At("HEAD"))
   private void archetypes$cauldronWaterDamage(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl, CallbackInfo ci){
      if(entity instanceof ServerPlayer playerEntity && playerEntity.level().getServer().getTickCount() % 20 == 0){
         PlayerArchetypeData profile = profile(playerEntity);
         if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && !playerEntity.hasEffect(MobEffects.WATER_BREATHING)){
            playerEntity.hurtServer(playerEntity.level(), world.damageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
            world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.GENERIC_BURN, playerEntity.getSoundSource(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
         }
      }
   }
}
