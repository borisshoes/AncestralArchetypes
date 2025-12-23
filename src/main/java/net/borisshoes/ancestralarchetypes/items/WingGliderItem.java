package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class WingGliderItem extends GliderItem{
   
   public WingGliderItem(Properties settings){
      super(ArchetypeRegistry.WING_GLIDER, ArchetypeRegistry.PARROT.getColor(), settings, ArchetypeRegistry.WING_GLIDER_TRIM_PATTERN);
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayer player)) return;
      
      if(stack.equals(player.getItemBySlot(EquipmentSlot.CHEST)) && player.isFallFlying()){
         if(player.isShiftKeyDown()){
            Vec3 lookingVec = player.getLookAngle();
            double d = 1.5;
            double e = 0.1;
            double f = 0.5;
            Vec3 velVec = player.getDeltaMovement();
            Vec3 deltaV = new Vec3(
                  lookingVec.x * e + (lookingVec.x * d - velVec.x) * f,
                  lookingVec.y * e + (lookingVec.y * d - velVec.y) * f,
                  lookingVec.z * e + (lookingVec.z * d - velVec.z) * f
            );
            double dp = deltaV.dot(lookingVec);
            if(dp < 0){
               deltaV = deltaV.subtract(lookingVec.scale(dp));
            }
            
            player.setDeltaMovement(velVec.add(deltaV));
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            PlayerArchetypeData profile = profile(player);

            world.sendParticles(new DustParticleOptions(profile.getGliderColor(),player.getRandom().nextFloat()+0.5f),false, false,
                  player.getX(), player.getY(), player.getZ(),
                  3, 0.15, 0.15, 0.15, 1);
            
            if(world.getServer().getTickCount() % 30 == 0){
               SoundUtils.playSound(world,player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.4f, 1.2f);
               world.sendParticles(ParticleTypes.POOF,false, false,
                     player.getX(), player.getY(), player.getZ(),
                     10, 0.05, 0.05, 0.05, 0.25);
            }
         }
      }
   }
}
