package net.borisshoes.ancestralarchetypes.entities;

import com.google.common.base.MoreObjects;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class LevitationBulletEntity extends Projectile implements PolymerEntity {
   @Nullable
   private EntityReference<Entity> target;
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
   
   public LevitationBulletEntity(EntityType<? extends LevitationBulletEntity> entityType, Level world){
      super(entityType, world);
   }
   
   public LevitationBulletEntity(Level world, LivingEntity owner, Entity target, Direction.Axis axis){
      this(ArchetypeRegistry.LEVITATION_BULLET_ENTITY,world);
      this.setOwner(owner);
      Vec3 vec3d = owner.getBoundingBox().getCenter();
      this.snapTo(vec3d.x, vec3d.y, vec3d.z, this.getYRot(), this.getXRot());
      this.target = EntityReference.of(target);
      this.direction = Direction.UP;
      this.changeTargetDirection(axis, target);
      this.maxSpeed = this.getRandom().nextDouble()*0.25 + 0.5;
      this.minSpeed = this.getRandom().nextDouble()*0.15 + 0.10;
   }
   
   @Override
   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }
   
   @Override
   protected void addAdditionalSaveData(ValueOutput view) {
      super.addAdditionalSaveData(view);
      if (this.target != null) {
         view.store("Target", UUIDUtil.CODEC, this.target.getUUID());
      }
      
      view.storeNullable("Dir", Direction.LEGACY_ID_CODEC, this.direction);
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
   protected void readAdditionalSaveData(ValueInput view) {
      super.readAdditionalSaveData(view);
      this.stepCount = view.getIntOr("Steps", 0);
      this.burstTicks = view.getIntOr("BurstCD", 0);
      this.burstCooldown = view.getIntOr("Steps", 0);
      this.targetX = view.getDoubleOr("TXD", 0.0);
      this.targetY = view.getDoubleOr("TYD", 0.0);
      this.targetZ = view.getDoubleOr("TZD", 0.0);
      this.maxSpeed = view.getDoubleOr("MaxSpeed",50.0);
      this.minSpeed = view.getDoubleOr("MinSpeed",15.0);
      this.direction = (Direction)view.read("Dir", Direction.LEGACY_ID_CODEC).orElse(null);
      this.target = EntityReference.read(view, "Target");
   }
   
   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }
   
   @Override
   public Direction getDirection() {
      return this.direction;
   }
   
   private void setDirection(@Nullable Direction direction) {
      this.direction = direction;
   }
   
   @Override
   public void checkDespawn() {
      if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
         this.discard();
      }
   }
   
   @Override
   protected double getDefaultGravity() {
      return 0.04;
   }
   
   @Override
   public void tick(){
      super.tick();
      Entity entity = !this.level().isClientSide() ? EntityReference.get(this.target, this.level(), Entity.class) : null;
      HitResult hitResult = null;
      if(!this.level().isClientSide()){
         if(entity == null){
            this.target = null;
            explode();
            discard();
         }
         if(entity == null || !entity.isAlive() || entity instanceof Player && entity.isSpectator()){
            this.applyGravity();
         }else{
            double farFull = 32.0;
            double burstRange = 12.0;
            Vec3 myPos = this.position();
            Vec3 tgtPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
            double dist = myPos.distanceTo(tgtPos);
            double t = Mth.clamp(dist / farFull, 0.0, 1.0);
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
            Vec3 dir = new Vec3(this.targetX, this.targetY, this.targetZ);
            double dm = dir.length();
            if(dm > 1.0E-6){
               Vec3 desired = dir.scale(1.0 / dm).scale(speedScale);
               Vec3 v = this.getDeltaMovement();
               this.setDeltaMovement(v.add(desired.subtract(v).scale(0.35)));
               Vec3 ahead = myPos.add(this.getDeltaMovement().scale(2.0));
               BlockHitResult pre = this.level().clip(new ClipContext(myPos, ahead, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
               if(pre.getType() == HitResult.Type.BLOCK){
                  Direction side = pre.getDirection();
                  Vec3 n = new Vec3(side.getStepX(), side.getStepY(), side.getStepZ());
                  Vec3 vv = this.getDeltaMovement();
                  Vec3 slide = vv.subtract(n.scale(vv.dot(n))).scale(0.85);
                  this.setDeltaMovement(slide);
                  this.stepCount = Math.min(this.stepCount, 6);
                  this.changeTargetDirection(side.getAxis(), entity);
               }
            }else{
               this.applyGravity();
            }
         }
         hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      }
      Vec3 vec3d = this.getDeltaMovement();
      this.setPos(this.position().add(vec3d));
      this.applyEffectsFromBlocks();
      if(this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) this.handlePortal();
      if(hitResult != null && this.isAlive() && hitResult.getType() != HitResult.Type.MISS) this.hitTargetOrDeflectSelf(hitResult);
      ProjectileUtil.rotateTowardsMovement(this, 0.5F);
      if(this.level().isClientSide()){
         this.level().addParticle(ParticleTypes.END_ROD, this.getX() - vec3d.x, this.getY() - vec3d.y + 0.15, this.getZ() - vec3d.z, 0.0, 0.0, 0.0);
      }else if(entity != null){
         if(this.stepCount > 0){
            this.stepCount--;
            if(this.stepCount == 0) this.changeTargetDirection(this.direction == null ? null : this.direction.getAxis(), entity);
         }
         if(this.direction != null){
            BlockPos blockPos = this.blockPosition();
            Direction.Axis axis = this.direction.getAxis();
            if(this.level().loadedAndEntityCanStandOn(blockPos.relative(this.direction), this)){
               this.changeTargetDirection(axis, entity);
            }else{
               BlockPos blockPos2 = entity.blockPosition();
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
         blockPos = this.blockPosition().below();
      }else{
         d = target.getBbHeight() * 0.5;
         blockPos = BlockPos.containing(target.getX(), target.getY() + d, target.getZ());
      }
      double e = blockPos.getX() + 0.5;
      double f = blockPos.getY() + d;
      double g = blockPos.getZ() + 0.5;
      Direction pick = null;
      Vec3 goal = new Vec3(e, f, g);
      if(!blockPos.closerToCenterThan(this.position(), 2.0)){
         BlockPos here = this.blockPosition();
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
            double nx = this.getX() + dir.getStepX();
            double ny = this.getY() + dir.getStepY();
            double nz = this.getZ() + dir.getStepZ();
            AABB nb = this.getBoundingBox().move(nx - this.getX(), ny - this.getY(), nz - this.getZ());
            if(!this.level().noCollision(this, nb)) continue;
            BlockHitResult line = this.level().clip(new ClipContext(new Vec3(nx, ny, nz), goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            double distScore = new Vec3(nx, ny, nz).distanceToSqr(goal);
            double blockPenalty = line.getType() == HitResult.Type.MISS ? 0.0 : 4.0;
            double continueBonus = this.direction != null && this.direction == dir ? -0.25 : 0.0;
            double score = distScore + blockPenalty + continueBonus;
            if(score < best){
               best = score;
               bestDir = dir;
            }
         }
         if(bestDir == null){
            Direction dir = Direction.getRandom(this.random);
            for(int i = 8; i > 0 && !this.level().noCollision(this, this.getBoundingBox().move(dir.getStepX(), dir.getStepY(), dir.getStepZ())); i--) dir = Direction.getRandom(this.random);
            bestDir = dir;
         }
         pick = bestDir;
         e = this.getX() + pick.getStepX();
         f = this.getY() + pick.getStepY();
         g = this.getZ() + pick.getStepZ();
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
      this.needsSync = true;
      int base = 6 + this.random.nextInt(6);
      int extra = Math.min(14, (int)(l * 1.25));
      this.stepCount = base + extra;
   }
   
   
   @Override
   protected boolean isAffectedByBlocks() {
      return !this.isRemoved();
   }
   
   @Override
   protected boolean canHitEntity(Entity entity) {
      return super.canHitEntity(entity) && !entity.noPhysics;
   }
   
   @Override
   public boolean isOnFire() {
      return false;
   }
   
   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 16384.0;
   }
   
   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }
   
   @Override
   protected void onHitEntity(EntityHitResult entityHitResult) {
      super.onHitEntity(entityHitResult);
      Entity entity = entityHitResult.getEntity();
      Entity entity2 = this.getOwner();
      LivingEntity livingEntity = entity2 instanceof LivingEntity ? (LivingEntity)entity2 : null;
      DamageSource damageSource = this.damageSources().mobProjectile(this, livingEntity);
      boolean bl = entity.hurtOrSimulate(damageSource, (float) CONFIG.getDouble(ArchetypeRegistry.LEVITATION_BULLET_DAMAGE));
      if (bl) {
         if (this.level() instanceof ServerLevel serverWorld) {
            EnchantmentHelper.doPostAttackEffects(serverWorld, entity, damageSource);
         }
         
         if (entity instanceof LivingEntity livingEntity2) {
            livingEntity2.addEffect(new MobEffectInstance(MobEffects.LEVITATION, CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_DURATION), CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_LEVEL)-1), MoreObjects.firstNonNull(entity2, this));
         }
      }
   }
   
   private void explode(){
      if(this.isRemoved()) return;
      ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
      this.playSound(SoundEvents.SHULKER_BULLET_HIT, 1.0F, 1.0F);
      double range = 1.5;
      List<LivingEntity> living = level().getEntitiesOfClass(LivingEntity.class,this.getBoundingBox().inflate(range*2)).stream().filter(e -> e.distanceTo(this) <= range).toList();
      for(LivingEntity livingEntity : living){
         livingEntity.addEffect(new MobEffectInstance(MobEffects.LEVITATION,CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_DURATION), CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_LEVEL)-1),MoreObjects.firstNonNull(this.getOwner(), this));
      }
   }
   
   @Override
   protected void onHitBlock(BlockHitResult blockHitResult) {
      super.onHitBlock(blockHitResult);
      explode();
      discard();
   }
   
   private void destroy() {
      this.discard();
      this.level().gameEvent(GameEvent.ENTITY_DAMAGE, this.position(), GameEvent.Context.of(this));
   }
   
   @Override
   protected void onHit(HitResult hitResult) {
      if(hitResult.getType() == HitResult.Type.ENTITY){
         EntityHitResult entityHitResult = (EntityHitResult)hitResult;
         Entity entity = entityHitResult.getEntity();
         if(entity instanceof LevitationBulletEntity) return;
      }
      super.onHit(hitResult);
      this.destroy();
   }
   
   @Override
   public boolean isPickable() {
      return true;
   }
   
   @Override
   public boolean hurtClient(DamageSource source) {
      return true;
   }
   
   @Override
   public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
      this.playSound(SoundEvents.SHULKER_BULLET_HURT, 1.0F, 1.0F);
      world.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
      this.destroy();
      return true;
   }
   
   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket packet) {
      super.recreateFromPacket(packet);
      this.setDeltaMovement(packet.getMovement());
   }
   
   @Override
   public EntityType<?> getPolymerEntityType(PacketContext packetContext){
      return EntityType.SHULKER_BULLET;
   }
}
