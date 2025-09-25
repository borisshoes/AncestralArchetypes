package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class WingGliderItem extends GliderItem{
   
   public WingGliderItem(Settings settings){
      super(ArchetypeRegistry.WING_GLIDER, ArchetypeRegistry.PARROT.getColor(), settings, ArchetypeRegistry.WING_GLIDER_TRIM_PATTERN);
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayerEntity player)) return;
      
      if(stack.equals(player.getEquippedStack(EquipmentSlot.CHEST)) && player.isGliding()){
         if(player.isSneaking()){
            Vec3d lookingVec = player.getRotationVector();
            double d = 1.5;
            double e = 0.1;
            double f = 0.5;
            Vec3d velVec = player.getVelocity();
            Vec3d deltaV = new Vec3d(
                  lookingVec.x * e + (lookingVec.x * d - velVec.x) * f,
                  lookingVec.y * e + (lookingVec.y * d - velVec.y) * f,
                  lookingVec.z * e + (lookingVec.z * d - velVec.z) * f
            );
            double dp = deltaV.dotProduct(lookingVec);
            if(dp < 0){
               deltaV = deltaV.subtract(lookingVec.multiply(dp));
            }
            
            player.setVelocity(velVec.add(deltaV));
            player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
            IArchetypeProfile profile = profile(player);

            world.spawnParticles(new DustParticleEffect(profile.getGliderColor(),player.getRandom().nextFloat()+0.5f),false, false,
                  player.getX(), player.getY(), player.getZ(),
                  3, 0.15, 0.15, 0.15, 1);
            
            if(world.getServer().getTicks() % 30 == 0){
               SoundUtils.playSound(world,player.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.4f, 1.2f);
               world.spawnParticles(ParticleTypes.POOF,false, false,
                     player.getX(), player.getY(), player.getZ(),
                     10, 0.05, 0.05, 0.05, 0.25);
            }
         }
      }
   }
}
