package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(LeveledCauldronBlock.class)
public class LeveledCauldronBlockMixin {
   
   @Inject(method = "onEntityCollision", at = @At("HEAD"))
   private void archetypes$cauldronWaterDamage(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler, CallbackInfo ci){
      if(entity instanceof ServerPlayerEntity playerEntity && playerEntity.getServer().getTicks() % 20 == 0){
         IArchetypeProfile profile = profile(playerEntity);
         if(profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER)){
            playerEntity.damage(playerEntity.getWorld(), world.getDamageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
            world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_GENERIC_BURN, playerEntity.getSoundCategory(), 0.4F, 2.0F + playerEntity.getRandom().nextFloat() * 0.4F);
         }
      }
   }
}
