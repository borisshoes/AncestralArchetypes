package net.borisshoes.ancestralarchetypes.entities;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.mixins.ThrownItemEntityAccessor;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class SnowblastEntity extends SnowballEntity implements PolymerEntity {
   public static final ItemStack VIEW_STACK = new ItemStack(Items.SNOWBALL);
   static{ VIEW_STACK.set(DataComponentTypes.ITEM_MODEL, Identifier.of(MOD_ID,"snow_blast_ball")); }
   
   public SnowblastEntity(EntityType<? extends SnowblastEntity> entityType, World world){
      super(entityType, world);
   }
   
   public SnowblastEntity(World world, LivingEntity owner, ItemStack itemStack){
      this(ArchetypeRegistry.SNOWBLAST_ENTITY, world);
      setPosition(owner.getX(), owner.getEyeY() - (double)0.1f, owner.getZ());
      setOwner(owner);
   }
   
   @Override
   public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial){
      data.add(new DataTracker.SerializedEntry<>(ThrownItemEntityAccessor.getITEM().id(), ThrownItemEntityAccessor.getITEM().dataType(), VIEW_STACK.copy()));
   }
   
   @Override
   protected void onCollision(HitResult hitResult){
      super.onCollision(hitResult);
    
      double range = CONFIG.getDouble(ArchetypeRegistry.SNOW_BLAST_RANGE);
      float damage = (float) CONFIG.getDouble(ArchetypeRegistry.SNOW_BLAST_DAMAGE);
      int duration = CONFIG.getInt(ArchetypeRegistry.SNOW_BLAST_SLOWNESS_DURATION);
      int lvl = CONFIG.getInt(ArchetypeRegistry.SNOW_BLAST_SLOWNESS_STRENGTH);
      Box box = this.getBoundingBox().expand(range*2);
      
      List<LivingEntity> living = getWorld().getNonSpectatingEntities(LivingEntity.class,box)
            .stream().filter(e -> e.distanceTo(this) <= range).toList();
      
      SoundUtils.playSound(getWorld(), BlockPos.ofFloored(hitResult.getPos()), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS,2.0f,0.5f);
      SoundUtils.playSound(getWorld(), BlockPos.ofFloored(hitResult.getPos()), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS,0.5f,0.5f);
      
      
      if(getWorld() instanceof ServerWorld world){
         for(BlockPos blockPos : BlockPos.iterateOutwards(BlockPos.ofFloored(hitResult.getPos()), (int)range + 1, (int)range + 1, (int)range + 1)){
            if(!blockPos.isWithinDistance(hitResult.getPos(),range)) continue;
            if(world.getBlockState(blockPos).isOf(Blocks.WATER)){
               world.setBlockState(blockPos, Blocks.FROSTED_ICE.getDefaultState());
            }
         }
         
         world.spawnParticles(ParticleTypes.SNOWFLAKE,hitResult.getPos().getX(),hitResult.getPos().getY(),hitResult.getPos().getZ(),50,0.25,0.25,0.25,0.5);
         world.spawnParticles(ParticleTypes.ITEM_SNOWBALL,hitResult.getPos().getX(),hitResult.getPos().getY(),hitResult.getPos().getZ(),50,0.25,0.25,0.25,0.5);
         for(LivingEntity livingEntity : living){
            livingEntity.damage(world,new DamageSource(world.getRegistryManager().getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(DamageTypes.FREEZE),this,this.getOwner()),damage);
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,duration,lvl-1),this.getOwner());
            world.spawnParticles(ParticleTypes.SNOWFLAKE,livingEntity.getX(),livingEntity.getY()+livingEntity.getHeight()/2.0,livingEntity.getZ(),25,livingEntity.getWidth()/2.0,livingEntity.getHeight()/2.0,livingEntity.getWidth()/2.0,0.1);
            world.spawnParticles(ParticleTypes.ITEM_SNOWBALL,livingEntity.getX(),livingEntity.getY()+livingEntity.getHeight()/2.0,livingEntity.getZ(),25,livingEntity.getWidth()/2.0,livingEntity.getHeight()/2.0,livingEntity.getWidth()/2.0,0.1);
//            if(livingEntity.getBlockStateAtPos().isIn(BlockTags.REPLACEABLE)){
//               world.setBlockState(livingEntity.getBlockPos(), Blocks.POWDER_SNOW.getDefaultState());
//            }
         }
      }
   }
   
   @Override
   public EntityType<?> getPolymerEntityType(PacketContext packetContext){
      return EntityType.SNOWBALL;
   }
}
