package net.borisshoes.ancestralarchetypes.entities;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.mixins.ThrowableItemProjectileAccessor;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class SnowblastEntity extends Snowball implements PolymerEntity {
   public static final ItemStack VIEW_STACK = new ItemStack(Items.SNOWBALL);
   static{ VIEW_STACK.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(MOD_ID,"snow_blast_ball")); }
   
   public SnowblastEntity(EntityType<? extends SnowblastEntity> entityType, Level world){
      super(entityType, world);
   }
   
   public SnowblastEntity(Level world, LivingEntity owner, ItemStack itemStack){
      this(ArchetypeRegistry.SNOWBLAST_ENTITY, world);
      setPos(owner.getX(), owner.getEyeY() - (double)0.1f, owner.getZ());
      setOwner(owner);
   }
   
   @Override
   public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial){
      data.add(new SynchedEntityData.DataValue<>(ThrowableItemProjectileAccessor.getDATA_ITEM_STACK().id(), ThrowableItemProjectileAccessor.getDATA_ITEM_STACK().serializer(), VIEW_STACK.copy()));
   }
   
   @Override
   protected void onHit(HitResult hitResult){
      super.onHit(hitResult);
    
      double range = CONFIG.getDouble(ArchetypeRegistry.SNOW_BLAST_RANGE);
      float damage = (float) CONFIG.getDouble(ArchetypeRegistry.SNOW_BLAST_DAMAGE);
      int duration = CONFIG.getInt(ArchetypeRegistry.SNOW_BLAST_SLOWNESS_DURATION);
      int lvl = CONFIG.getInt(ArchetypeRegistry.SNOW_BLAST_SLOWNESS_STRENGTH);
      AABB box = this.getBoundingBox().inflate(range*2);
      
      List<LivingEntity> living = level().getEntitiesOfClass(LivingEntity.class,box)
            .stream().filter(e -> e.distanceTo(this) <= range).toList();
      
      SoundUtils.playSound(level(), BlockPos.containing(hitResult.getLocation()), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS,2.0f,0.5f);
      SoundUtils.playSound(level(), BlockPos.containing(hitResult.getLocation()), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS,0.5f,0.5f);
      
      
      if(level() instanceof ServerLevel world){
         for(BlockPos blockPos : BlockPos.withinManhattan(BlockPos.containing(hitResult.getLocation()), (int)range + 1, (int)range + 1, (int)range + 1)){
            if(!blockPos.closerToCenterThan(hitResult.getLocation(),range)) continue;
            if(world.getBlockState(blockPos).is(Blocks.WATER)){
               world.setBlockAndUpdate(blockPos, Blocks.FROSTED_ICE.defaultBlockState());
            }
         }
         
         world.sendParticles(ParticleTypes.SNOWFLAKE,hitResult.getLocation().x(),hitResult.getLocation().y(),hitResult.getLocation().z(),50,0.25,0.25,0.25,0.5);
         world.sendParticles(ParticleTypes.ITEM_SNOWBALL,hitResult.getLocation().x(),hitResult.getLocation().y(),hitResult.getLocation().z(),50,0.25,0.25,0.25,0.5);
         for(LivingEntity livingEntity : living){
            livingEntity.hurtServer(world,new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FREEZE),this,this.getOwner()),damage);
            livingEntity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,duration,lvl-1),this.getOwner());
            world.sendParticles(ParticleTypes.SNOWFLAKE,livingEntity.getX(),livingEntity.getY()+livingEntity.getBbHeight()/2.0,livingEntity.getZ(),25,livingEntity.getBbWidth()/2.0,livingEntity.getBbHeight()/2.0,livingEntity.getBbWidth()/2.0,0.1);
            world.sendParticles(ParticleTypes.ITEM_SNOWBALL,livingEntity.getX(),livingEntity.getY()+livingEntity.getBbHeight()/2.0,livingEntity.getZ(),25,livingEntity.getBbWidth()/2.0,livingEntity.getBbHeight()/2.0,livingEntity.getBbWidth()/2.0,0.1);
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
