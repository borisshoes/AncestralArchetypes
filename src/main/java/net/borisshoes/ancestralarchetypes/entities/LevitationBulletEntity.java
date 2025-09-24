package net.borisshoes.ancestralarchetypes.entities;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class LevitationBulletEntity extends ProjectileEntity implements PolymerEntity {
   @Nullable
   private LazyEntityReference<Entity> target;
   @Nullable
   private Direction direction;
   private int stepCount;
   private int burstTicks;
   private int burstCooldown;
   private double targetX;
   private double targetY;
   private double targetZ;
   private double maxSpeed;
   private double minSpeed;
   
   public LevitationBulletEntity(EntityType<? extends LevitationBulletEntity> entityType, World world){
      super(entityType, world);
   }
   
   public LevitationBulletEntity(World world, LivingEntity owner, Entity target, Direction.Axis axis){
      this(ArchetypeRegistry.LEVITATION_BULLET_ENTITY,world);
      this.setOwner(owner);
      Vec3d vec3d = owner.getBoundingBox().getCenter();
      this.refreshPositionAndAngles(vec3d.x, vec3d.y, vec3d.z, this.getYaw(), this.getPitch());
      this.target = new LazyEntityReference<>(target);
      this.direction = Direction.UP;
      this.changeTargetDirection(axis, target);
      this.maxSpeed = this.getRandom().nextDouble()*0.25 + 0.5;
      this.minSpeed = this.getRandom().nextDouble()*0.15 + 0.10;
   }
   
   @Override
   public SoundCategory getSoundCategory() {
      return SoundCategory.HOSTILE;
   }
   
   @Override
   protected void writeCustomData(WriteView view) {
      super.writeCustomData(view);
      if (this.target != null) {
         view.put("Target", Uuids.INT_STREAM_CODEC, this.target.getUuid());
      }
      
      view.putNullable("Dir", Direction.INDEX_CODEC, this.direction);
      view.putInt("Steps", this.stepCount);
      view.putInt("BurstTicks", this.burstTicks);
      view.putInt("BurstCD", this.burstCooldown);
      view.putDouble("TXD", this.targetX);
      view.putDouble("TYD", this.targetY);
      view.putDouble("TZD", this.targetZ);
      view.putDouble("MaxSpeed",this.maxSpeed);
      view.putDouble("MinSpeed",this.minSpeed);
   }
   
   @Override
   protected void readCustomData(ReadView view) {
      super.readCustomData(view);
      this.stepCount = view.getInt("Steps", 0);
      this.burstTicks = view.getInt("BurstCD", 0);
      this.burstCooldown = view.getInt("Steps", 0);
      this.targetX = view.getDouble("TXD", 0.0);
      this.targetY = view.getDouble("TYD", 0.0);
      this.targetZ = view.getDouble("TZD", 0.0);
      this.maxSpeed = view.getDouble("MaxSpeed",50.0);
      this.minSpeed = view.getDouble("MinSpeed",15.0);
      this.direction = (Direction)view.read("Dir", Direction.INDEX_CODEC).orElse(null);
      this.target = LazyEntityReference.fromData(view, "Target");
   }
   
   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
   }
   
   @Nullable
   private Direction getDirection() {
      return this.direction;
   }
   
   private void setDirection(@Nullable Direction direction) {
      this.direction = direction;
   }
   
   @Override
   public void checkDespawn() {
      if (this.getWorld().getDifficulty() == Difficulty.PEACEFUL) {
         this.discard();
      }
   }
   
   @Override
   protected double getGravity() {
      return 0.04;
   }
   
   @Override
   public void tick(){
      super.tick();
      Entity entity = !this.getWorld().isClient() ? LazyEntityReference.resolve(this.target, this.getWorld(), Entity.class) : null;
      HitResult hitResult = null;
      if(!this.getWorld().isClient){
         if(entity == null){
            this.target = null;
            explode();
            discard();
         }
         if(entity == null || !entity.isAlive() || entity instanceof PlayerEntity && entity.isSpectator()){
            this.applyGravity();
         }else{
            double farFull = 32.0;
            double burstRange = 12.0;
            Vec3d myPos = this.getPos();
            Vec3d tgtPos = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ());
            double dist = myPos.distanceTo(tgtPos);
            double t = MathHelper.clamp(dist / farFull, 0.0, 1.0);
            double speedScale = minSpeed + t * (maxSpeed - minSpeed);
            if(dist <= burstRange){
               if(burstTicks > 0){
                  speedScale *= 1.6;
                  burstTicks--;
               }else if(burstCooldown == 0 && this.random.nextFloat() < 0.08F){
                  burstTicks = 6 + this.random.nextInt(5);
                  burstCooldown = 30 + this.random.nextInt(20);
               }
            }
            if(burstCooldown > 0) burstCooldown--;
            Vec3d dir = new Vec3d(this.targetX, this.targetY, this.targetZ);
            double dm = dir.length();
            if(dm > 1.0E-6){
               Vec3d desired = dir.multiply(1.0 / dm).multiply(speedScale);
               Vec3d v = this.getVelocity();
               this.setVelocity(v.add(desired.subtract(v).multiply(0.35)));
               Vec3d ahead = myPos.add(this.getVelocity().multiply(2.0));
               BlockHitResult pre = this.getWorld().raycast(new RaycastContext(myPos, ahead, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
               if(pre.getType() == HitResult.Type.BLOCK){
                  Direction side = pre.getSide();
                  Vec3d n = new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
                  Vec3d vv = this.getVelocity();
                  Vec3d slide = vv.subtract(n.multiply(vv.dotProduct(n))).multiply(0.85);
                  this.setVelocity(slide);
                  this.stepCount = Math.min(this.stepCount, 6);
                  this.changeTargetDirection(side.getAxis(), entity);
               }
            }else{
               this.applyGravity();
            }
         }
         hitResult = ProjectileUtil.getCollision(this, this::canHit);
      }
      Vec3d vec3d = this.getVelocity();
      this.setPosition(this.getPos().add(vec3d));
      this.tickBlockCollision();
      if(this.portalManager != null && this.portalManager.isInPortal()) this.tickPortalTeleportation();
      if(hitResult != null && this.isAlive() && hitResult.getType() != HitResult.Type.MISS) this.hitOrDeflect(hitResult);
      ProjectileUtil.setRotationFromVelocity(this, 0.5F);
      if(this.getWorld().isClient){
         this.getWorld().addParticleClient(ParticleTypes.END_ROD, this.getX() - vec3d.x, this.getY() - vec3d.y + 0.15, this.getZ() - vec3d.z, 0.0, 0.0, 0.0);
      }else if(entity != null){
         if(this.stepCount > 0){
            this.stepCount--;
            if(this.stepCount == 0) this.changeTargetDirection(this.direction == null ? null : this.direction.getAxis(), entity);
         }
         if(this.direction != null){
            BlockPos blockPos = this.getBlockPos();
            Direction.Axis axis = this.direction.getAxis();
            if(this.getWorld().isTopSolid(blockPos.offset(this.direction), this)){
               this.changeTargetDirection(axis, entity);
            }else{
               BlockPos blockPos2 = entity.getBlockPos();
               if(axis == Direction.Axis.X && blockPos.getX() == blockPos2.getX() || axis == Direction.Axis.Z && blockPos.getZ() == blockPos2.getZ() || axis == Direction.Axis.Y && blockPos.getY() == blockPos2.getY()){
                  this.changeTargetDirection(axis, entity);
               }
            }
         }
      }
   }
   
   private void changeTargetDirection(@Nullable Direction.Axis axis, @Nullable Entity target){
      double d = 0.5;
      BlockPos blockPos;
      if(target == null){
         blockPos = this.getBlockPos().down();
      }else{
         d = target.getHeight() * 0.5;
         blockPos = BlockPos.ofFloored(target.getX(), target.getY() + d, target.getZ());
      }
      double e = blockPos.getX() + 0.5;
      double f = blockPos.getY() + d;
      double g = blockPos.getZ() + 0.5;
      Direction pick = null;
      Vec3d goal = new Vec3d(e, f, g);
      if(!blockPos.isWithinDistance(this.getPos(), 2.0)){
         BlockPos here = this.getBlockPos();
         java.util.List<Direction> cand = new java.util.ArrayList<>(6);
         if(axis != Direction.Axis.X){
            if(here.getX() < blockPos.getX()) cand.add(Direction.EAST);
            else if(here.getX() > blockPos.getX()) cand.add(Direction.WEST);
         }
         if(axis != Direction.Axis.Y){
            if(here.getY() < blockPos.getY()) cand.add(Direction.UP);
            else if(here.getY() > blockPos.getY()) cand.add(Direction.DOWN);
         }
         if(axis != Direction.Axis.Z){
            if(here.getZ() < blockPos.getZ()) cand.add(Direction.SOUTH);
            else if(here.getZ() > blockPos.getZ()) cand.add(Direction.NORTH);
         }
         double best = Double.POSITIVE_INFINITY;
         Direction bestDir = null;
         for(Direction dir : cand){
            double nx = this.getX() + dir.getOffsetX();
            double ny = this.getY() + dir.getOffsetY();
            double nz = this.getZ() + dir.getOffsetZ();
            Box nb = this.getBoundingBox().offset(nx - this.getX(), ny - this.getY(), nz - this.getZ());
            if(!this.getWorld().isSpaceEmpty(this, nb)) continue;
            BlockHitResult line = this.getWorld().raycast(new RaycastContext(new Vec3d(nx, ny, nz), goal, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
            double distScore = new Vec3d(nx, ny, nz).squaredDistanceTo(goal);
            double blockPenalty = line.getType() == HitResult.Type.MISS ? 0.0 : 4.0;
            double continueBonus = this.direction != null && this.direction == dir ? -0.25 : 0.0;
            double score = distScore + blockPenalty + continueBonus;
            if(score < best){
               best = score;
               bestDir = dir;
            }
         }
         if(bestDir == null){
            Direction dir = Direction.random(this.random);
            for(int i = 8; i > 0 && !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ())); i--) dir = Direction.random(this.random);
            bestDir = dir;
         }
         pick = bestDir;
         e = this.getX() + pick.getOffsetX();
         f = this.getY() + pick.getOffsetY();
         g = this.getZ() + pick.getOffsetZ();
      }
      this.setDirection(pick);
      double h = e - this.getX();
      double j = f - this.getY();
      double k = g - this.getZ();
      double l = Math.sqrt(h * h + j * j + k * k);
      if(l == 0.0){
         this.targetX = 0.0;
         this.targetY = 0.0;
         this.targetZ = 0.0;
      }else{
         double inv = 1.0 / l;
         this.targetX = h * inv;
         this.targetY = j * inv;
         this.targetZ = k * inv;
      }
      this.velocityDirty = true;
      int base = 6 + this.random.nextInt(6);
      int extra = Math.min(14, (int)(l * 1.25));
      this.stepCount = base + extra;
   }
   
   
   @Override
   protected boolean shouldTickBlockCollision() {
      return !this.isRemoved();
   }
   
   @Override
   protected boolean canHit(Entity entity) {
      return super.canHit(entity) && !entity.noClip;
   }
   
   @Override
   public boolean isOnFire() {
      return false;
   }
   
   @Override
   public boolean shouldRender(double distance) {
      return distance < 16384.0;
   }
   
   @Override
   public float getBrightnessAtEyes() {
      return 1.0F;
   }
   
   @Override
   protected void onEntityHit(EntityHitResult entityHitResult) {
      super.onEntityHit(entityHitResult);
      Entity entity = entityHitResult.getEntity();
      Entity entity2 = this.getOwner();
      LivingEntity livingEntity = entity2 instanceof LivingEntity ? (LivingEntity)entity2 : null;
      DamageSource damageSource = this.getDamageSources().mobProjectile(this, livingEntity);
      boolean bl = entity.sidedDamage(damageSource, (float) CONFIG.getDouble(ArchetypeRegistry.LEVITATION_BULLET_DAMAGE));
      if (bl) {
         if (this.getWorld() instanceof ServerWorld serverWorld) {
            EnchantmentHelper.onTargetDamaged(serverWorld, entity, damageSource);
         }
         
         if (entity instanceof LivingEntity livingEntity2) {
            livingEntity2.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_DURATION), CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_LEVEL)-1), MoreObjects.firstNonNull(entity2, this));
         }
      }
   }
   
   private void explode(){
      if(this.isRemoved()) return;
      ((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
      this.playSound(SoundEvents.ENTITY_SHULKER_BULLET_HIT, 1.0F, 1.0F);
      double range = 1.5;
      List<LivingEntity> living = getWorld().getNonSpectatingEntities(LivingEntity.class,this.getBoundingBox().expand(range*2)).stream().filter(e -> e.distanceTo(this) <= range).toList();
      for(LivingEntity livingEntity : living){
         livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION,CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_DURATION), CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_LEVEL)-1),MoreObjects.firstNonNull(this.getOwner(), this));
      }
   }
   
   @Override
   protected void onBlockHit(BlockHitResult blockHitResult) {
      super.onBlockHit(blockHitResult);
      explode();
      discard();
   }
   
   private void destroy() {
      this.discard();
      this.getWorld().emitGameEvent(GameEvent.ENTITY_DAMAGE, this.getPos(), GameEvent.Emitter.of(this));
   }
   
   @Override
   protected void onCollision(HitResult hitResult) {
      if(hitResult.getType() == HitResult.Type.ENTITY){
         EntityHitResult entityHitResult = (EntityHitResult)hitResult;
         Entity entity = entityHitResult.getEntity();
         if(entity instanceof LevitationBulletEntity) return;
      }
      super.onCollision(hitResult);
      this.destroy();
   }
   
   @Override
   public boolean canHit() {
      return true;
   }
   
   @Override
   public boolean clientDamage(DamageSource source) {
      return true;
   }
   
   @Override
   public boolean damage(ServerWorld world, DamageSource source, float amount) {
      this.playSound(SoundEvents.ENTITY_SHULKER_BULLET_HURT, 1.0F, 1.0F);
      world.spawnParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
      this.destroy();
      return true;
   }
   
   @Override
   public void onSpawnPacket(EntitySpawnS2CPacket packet) {
      super.onSpawnPacket(packet);
      double d = packet.getVelocityX();
      double e = packet.getVelocityY();
      double f = packet.getVelocityZ();
      this.setVelocity(d, e, f);
   }
   
   @Override
   public EntityType<?> getPolymerEntityType(PacketContext packetContext){
      return EntityType.SHULKER_BULLET;
   }
}
