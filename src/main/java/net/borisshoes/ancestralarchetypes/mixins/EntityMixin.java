package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.PLAYER_MOVEMENT_TRACKER;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(Entity.class)
public class EntityMixin {
   
   @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onEntityLand(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;)V"))
   private void archetypes_onEntityLand(MovementType type, Vec3d movement, CallbackInfo ci, @Local Block block, @Local BlockState state){
      Entity entity = (Entity)(Object) this;
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getWorld();
         
         if(profile.hasAbility(ArchetypeRegistry.BOUNCY) && !player.isDead()){
            if (!player.bypassesLandingEffects()) {
               Vec3d oldVel = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
               if (oldVel.y < 0.0) {
                  double newY = oldVel.y > -0.425 ? 0 : -0.9*oldVel.y;
                  Vec3d newVel = new Vec3d(oldVel.x, newY, oldVel.z);
                  player.fallDistance = 0;
                  player.setVelocity(newVel);
                  player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getId(), newVel));
               }
            }
         }
      }
   }
}
