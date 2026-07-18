package net.borisshoes.ancestralarchetypes.entities;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeParticles;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.mixins.InteractionAccessor;
import net.borisshoes.borislib.conditions.ConditionInstance;
import net.borisshoes.borislib.conditions.Conditions;
import net.borisshoes.borislib.utils.AlgoUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class CreakingHeartEntity extends LivingEntity implements PolymerEntity, OwnableEntity {
   
   private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(CreakingHeartEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(CreakingHeartEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_RESPONSE_ID = SynchedEntityData.defineId(CreakingHeartEntity.class, EntityDataSerializers.BOOLEAN);
   
   private @Nullable EntityReference<LivingEntity> owner;
   private boolean initializedAttributes = false;
   private long chunkTicketExpiryTicks = 0L;
   private int rangeWarnCooldown = 0;
   private int hurtWarnCooldown = 0;
   @Nullable
   private DisplayManager displayManager = null;
   
   public CreakingHeartEntity(EntityType<? extends CreakingHeartEntity> type, Level level){
      super(type, level);
   }
   
   @Override
   public EntityType<?> getPolymerEntityType(PacketContext context){
      return EntityTypes.INTERACTION;
   }
   
   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder){
      super.defineSynchedData(builder);
      float size = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE);
      builder.define(DATA_WIDTH_ID, size);
      builder.define(DATA_HEIGHT_ID, size);
      builder.define(DATA_RESPONSE_ID, true);
   }
   
   @Override
   public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial){
      float size = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE);
      data.add(new SynchedEntityData.DataValue<>(InteractionAccessor.getDATA_HEIGHT_ID().id(), InteractionAccessor.getDATA_HEIGHT_ID().serializer(), size));
      data.add(new SynchedEntityData.DataValue<>(InteractionAccessor.getDATA_WIDTH_ID().id(), InteractionAccessor.getDATA_WIDTH_ID().serializer(), size));
      data.add(new SynchedEntityData.DataValue<>(InteractionAccessor.getDATA_RESPONSE_ID().id(), InteractionAccessor.getDATA_RESPONSE_ID().serializer(), true));
   }
   
   @Override
   public @NonNull HumanoidArm getMainArm(){
      return HumanoidArm.RIGHT;
   }
   
   @Override
   public @Nullable EntityReference<LivingEntity> getOwnerReference(){
      return this.owner;
   }
   
   public void setOwner(LivingEntity entity){
      this.owner = EntityReference.of(entity);
   }
   
   public void setOwner(UUID entity){
      this.owner = EntityReference.of(entity);
   }
   
   private void initializeAttributes(){
      getAttribute(Attributes.MAX_HEALTH).setBaseValue(CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_HEALTH));
      getAttribute(Attributes.ARMOR).setBaseValue(CONFIG.getDouble(ArchetypeRegistry.CREAKING_HEART_ARMOR));
      getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(CONFIG.getDouble(ArchetypeRegistry.CREAKING_HEART_TOUGHNESS));
      getAttribute(Attributes.SCALE).setBaseValue(CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE));
      getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0f);
      getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(1.0f);
      setHealth((float) getAttribute(Attributes.MAX_HEALTH).getValue());
      setNoGravity(true);
      initializedAttributes = true;
   }
   
   public static AttributeSupplier.Builder createHeartAttributes(){
      return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0f)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.ARMOR_TOUGHNESS, 10.0)
            .add(Attributes.SCALE, 1.0f)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0f)
            .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0f);
   }
   
   @Override
   public void tick(){
      if(!initializedAttributes) initializeAttributes();
      if(displayManager == null && level() instanceof ServerLevel){
         displayManager = new DisplayManager(this);
      }
      if(displayManager != null) displayManager.tick();
      super.tick();
      if(rangeWarnCooldown > 0) rangeWarnCooldown--;
      if(hurtWarnCooldown > 0) hurtWarnCooldown--;
      
      try{
         if(this.isAlive()){
            BlockPos blockPos = BlockPos.containing(this.position());
            ChunkPos chunkPos = this.chunkPosition();
            int chunkX = SectionPos.blockToSectionCoord(this.position().x());
            int chunkZ = SectionPos.blockToSectionCoord(this.position().z());
            if((--this.chunkTicketExpiryTicks <= 0L || chunkX != SectionPos.blockToSectionCoord(blockPos.getX()) || chunkZ != SectionPos.blockToSectionCoord(blockPos.getZ())) && level() instanceof ServerLevel serverWorld){
               serverWorld.resetEmptyTime();
               this.chunkTicketExpiryTicks = ServerPlayer.placeEnderPearlTicket(serverWorld, chunkPos) - 1L;
            }
            
            LivingEntity owningEntity = getOwner();
            UUID ownerUUID = getOwnerReference() != null ? getOwnerReference().getUUID() : null;
            PlayerArchetypeData ownerProfile = ownerUUID != null ? profile(ownerUUID) : null;
            // Suppress the out-of-range self-destruct while a respawn is in progress OR during the post-finalize
            // grace window (the TeleportTransition may not have fully settled player.level() yet).
            boolean anchoringRespawn = ownerProfile != null &&
                  (ownerProfile.isCreakingRespawnActive() || ownerProfile.getCreakingRespawnGraceTicks() > 0);
            
            if(anchoringRespawn){
               // Anchoring a respawn: charge up and completely suppress the out-of-range self-destruct. The owner is
               // dead or watching the respawn cutscene, so distance checks and buffs are meaningless right now.
               doChargingParticles();
               if(owningEntity instanceof ServerPlayer player && !player.hasDisconnected()){
                  PlayerArchetypeData profile = profile(player);
                  if(!profile.hasAbility(ArchetypeRegistry.CREAKING_HEART)){
                     discard();
                     return;
                  }else if(!Objects.equals(profile.getCreakingHeart(), this)){
                     profile.setCreakingHeart(this);
                  }
               }
            }else if(owningEntity instanceof ServerPlayer player && !player.hasDisconnected()){
               PlayerArchetypeData profile = profile(player);
               if(!profile.hasAbility(ArchetypeRegistry.CREAKING_HEART)){
                  discard();
                  return;
               }else if(!Objects.equals(profile.getCreakingHeart(), this)){
                  profile.setCreakingHeart(this);
               }
               double range = CONFIG.getDouble(ArchetypeRegistry.CREAKING_HEART_RANGE);
               double lenience = 5;
               double distance = player.distanceTo(this);
               boolean inDimension = player.level().dimension().identifier().equals(this.level().dimension().identifier());
               if(distance <= range && inDimension){ // Buffs
                  float resist = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_RESISTANCE);
                  float strength = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_STRENGTH);
                  
                  Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
                        Conditions.FORTITUDE,
                        archetypesId("creaking_fortitude"),
                        10, -resist,
                        true, false, true,
                        AttributeModifier.Operation.ADD_VALUE, null));
                  Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
                        Conditions.MIGHT,
                        archetypesId("creaking_might"),
                        10, strength,
                        true, true, true,
                        AttributeModifier.Operation.ADD_VALUE, null));
               }else if(distance <= range + lenience && inDimension){ // Progressive slowness
                  float minSlow = 0.5f;
                  float maxSlow = 0.9f;
                  float slow = (float) ((distance - range) / lenience * (maxSlow - minSlow)) + minSlow;
                  Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
                        Conditions.TORPOR,
                        archetypesId("creaking_range_slow"),
                        10, slow,
                        false, false, true,
                        AttributeModifier.Operation.ADD_VALUE, null));
                  if(rangeWarnCooldown <= 0){
                     player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.creaking_heart_leaving_range").withColor(TextColor.RED.getValue()), false);
                     rangeWarnCooldown = 100;
                  }
               }else{ // Break
                  kill((ServerLevel) this.level());
               }
               
               if(this.tickCount % 3 == 0){
                  boolean reducedParticles = CONFIG.getBoolean(ArchetypeRegistry.REDUCED_PARTICLES);
                  double visibilityRange = 8;
                  if(distance > (range - visibilityRange) && distance < (range + visibilityRange)){
                     DustParticleOptions p1 = new DustParticleOptions(0xfc7812, 0.75f);
                     double theta = player.level().getServer().getTickCount() * Mth.TWO_PI / 57.12;
                     int count = reducedParticles ? (int) (5 * range * range) : (int) (10 * range * range);
                     ArchetypeParticles.localSphere(player, this.position().add(0, this.getBbHeight() / 2, 0), p1, range, visibilityRange, count, 1, 0.1, 0, theta);
                  }
                  if(distance > (range - visibilityRange + lenience) && distance < (range + visibilityRange + lenience)){
                     DustParticleOptions p2 = new DustParticleOptions(0xf72800, 1.5f);
                     double theta = -player.level().getServer().getTickCount() * Mth.TWO_PI / 57.12;
                     int count = reducedParticles ? (int) (5 * range * range) : (int) (10 * range * range);
                     ArchetypeParticles.localSphere(player, this.position().add(0, this.getBbHeight() / 2, 0), p2, range + lenience, visibilityRange, count, 1, 0.1, 0, theta);
                  }
               }
               
               if(random.nextFloat() < 0.25f) doTrailParticles(player, 1);
            }
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.error(e.getMessage());
         e.printStackTrace();
      }
   }
   
   @Override
   public @Nullable PlayerTeam getTeam(){
      PlayerTeam ownTeam = super.getTeam();
      if(ownTeam != null){
         return ownTeam;
      }
      
      LivingEntity owner = this.getRootOwner();
      if(owner != null){
         return owner.getTeam();
      }
      
      return null;
   }
   
   @Override
   public boolean considersEntityAsAlly(final @NonNull Entity other){
      LivingEntity owner = this.getRootOwner();
      if(other == owner){
         return true;
      }
      
      if(owner != null){
         return owner.isAlliedTo(other);
      }
      
      return super.considersEntityAsAlly(other);
   }
   
   public void doTrailParticles(ServerPlayer player, int amount){
      for(int i = 0; i < amount; i++){
         AABB thisBox = this.getBoundingBox();
         AABB playerBox = player.getBoundingBox();
         Vec3 thisPos = thisBox.getMinPosition().add(random.nextDouble() * thisBox.getXsize(), random.nextDouble() * thisBox.getYsize(), random.nextDouble() * thisBox.getZsize());
         Vec3 playerPos = playerBox.getMinPosition().add(random.nextDouble() * playerBox.getXsize(), random.nextDouble() * playerBox.getYsize(), random.nextDouble() * playerBox.getZsize());
         TrailParticleOption trailTo = new TrailParticleOption(playerPos, 0xfc7812, this.random.nextInt(40) + 20);
         TrailParticleOption trailFrom = new TrailParticleOption(thisPos, 0xfc7812, this.random.nextInt(40) + 20);
         player.level().sendParticles(trailTo, true, true, thisPos.x, thisPos.y, thisPos.z, 1, 0.0, 0.0, 0.0, 0.0);
         player.level().sendParticles(trailFrom, true, true, playerPos.x, playerPos.y, playerPos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }
   
   public void doChargingParticles(){
      if(!(level() instanceof ServerLevel serverLevel)) return;
      Vec3 center = this.position().add(0, this.getBbHeight() / 2.0, 0);
      float entityScale = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE);
      
      // Compute how much of the respawn timer remains (1.0 = just died, 0.0 = timer elapsed).
      float timerFraction = 1.0f;
      UUID ownerUUID = getOwnerReference() != null ? getOwnerReference().getUUID() : null;
      if(ownerUUID != null){
         PlayerArchetypeData ownerProfile = profile(ownerUUID);
         long deathTick = ownerProfile.getCreakingRespawnDeathTick();
         if(deathTick > 0L){
            int total = CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_RESPAWN_TIMER);
            long elapsed = serverLevel.getServer().getTickCount() - deathTick;
            timerFraction = (float) Math.max(0.0, 1.0 - (double) elapsed / total);
         }
      }
      
      // Radius: starts at 2× the entity's scale, collapses to 0.25× as the timer runs out.
      double baseRadius = entityScale * (0.25 + timerFraction * 1.75);
      
      for(int i = 0; i < 4; i++){
         double angle = random.nextDouble() * Mth.TWO_PI;
         double r = baseRadius * (0.65 + random.nextDouble() * 0.35);
         double px = center.x + Math.cos(angle) * r;
         double pz = center.z + Math.sin(angle) * r;
         double py = center.y - 0.4 * entityScale + random.nextDouble() * entityScale * 0.8;
         serverLevel.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, px, py, pz, 1, 0.0, 0.05, 0.0, 0.0);
      }
   }
   
   @Override
   public void knockback(double power, double xd, double zd, DamageSource source, float damage){
   }
   
   @Override
   public boolean isPushable(){
      return false;
   }
   
   @Override
   public void push(final double xa, final double ya, final double za){
   }
   
   @Override
   protected @Nullable SoundEvent getHurtSound(DamageSource source){
      return SoundEvents.CREAKING_SWAY;
   }
   
   @Override
   protected @Nullable SoundEvent getDeathSound(){
      return SoundEvents.CREAKING_DEATH;
   }
   
   @Override
   public boolean hurtServer(ServerLevel level, DamageSource source, float damage){
      boolean superReturn = super.hurtServer(level, source, damage);
      if(superReturn && displayManager != null) displayManager.triggerShake();
      LivingEntity owningEntity = getOwner();
      if(superReturn && this.isAlive() && owningEntity instanceof ServerPlayer player && !player.hasDisconnected()){
         doTrailParticles(player, 20);
         
         if(hurtWarnCooldown <= 0){
            player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.creaking_heart_hurt").withColor(TextColor.RED.getValue()), false);
            hurtWarnCooldown = 100;
         }
      }
      return superReturn;
   }
   
   @Override
   public void die(@NonNull DamageSource source){
      super.die(source);
      
      LivingEntity owningEntity = getOwner();
      if(owningEntity instanceof ServerPlayer player && !player.hasDisconnected()){
         float slow = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SLOWNESS);
         int slowDur = CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_SLOWNESS_DURATION);
         float weak = -CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_WEAKNESS);
         int weakDur = CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_WEAKNESS_DURATION);
         Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
               Conditions.TORPOR,
               archetypesId("creaking_slow"),
               slowDur, slow,
               true, true, false,
               AttributeModifier.Operation.ADD_VALUE, null));
         Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
               Conditions.FEEBLE,
               archetypesId("creaking_weak"),
               weakDur, weak,
               true, true, false,
               AttributeModifier.Operation.ADD_VALUE, null));
         
         MutableComponent text;
         if(source.getEntity() != null){
            text = Component.translatable("text.ancestralarchetypes.creaking_heart_destroyed_by", source.getEntity().getName());
         }else{
            text = Component.translatable("text.ancestralarchetypes.creaking_heart_destroyed");
         }
         player.sendSystemMessage(text.withColor(TextColor.RED.getValue()), false);
         playSound(SoundEvents.CREAKING_DEATH, 2, 0.5f);
         profile(player).setAbilityCooldown(ArchetypeRegistry.CREAKING_HEART, CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_COOLDOWN));
         float entityScale = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE) * 0.5f;
         ((ServerLevel) this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.CREAKING_HEART.defaultBlockState()),
               this.getX(), this.getY() + this.getBbHeight() / 2, this.getZ(), 125, entityScale, entityScale, entityScale, 1);
      }
      
      if(displayManager != null){
         displayManager.setInvisible();
         displayManager.destroy();
         displayManager = null;
      }
   }
   
   @Override
   public void onRemoval(Entity.RemovalReason reason){
      super.onRemoval(reason);
      if(displayManager != null){
         displayManager.destroy();
         displayManager = null;
      }
   }
   
   @Override
   protected void addAdditionalSaveData(@NonNull ValueOutput view){
      super.addAdditionalSaveData(view);
      if(getOwnerReference() != null)
         view.putString("owner", getOwnerReference().getUUID().toString());
   }
   
   @Override
   protected void readAdditionalSaveData(@NonNull ValueInput view){
      super.readAdditionalSaveData(view);
      if(view.contains("owner"))
         setOwner(AlgoUtils.getUUID(view.getString("owner").orElse("")));
   }
   
   /**
    * Only lets players WITH the resource pack start watching.
    */
   private static final class PackFilterHolder extends ElementHolder {
      @Override
      public boolean startWatching(ServerGamePacketListenerImpl player){
         if(!PolymerResourcePackUtils.hasMainPack(player.getPlayer())) return false;
         return super.startWatching(player);
      }
   }
   
   /**
    * Only lets players WITHOUT the resource pack start watching.
    */
   private static final class NoPackFilterHolder extends ElementHolder {
      @Override
      public boolean startWatching(ServerGamePacketListenerImpl player){
         if(PolymerResourcePackUtils.hasMainPack(player.getPlayer())) return false;
         return super.startWatching(player);
      }
   }
   
   private static final class DisplayManager {
      private static final int BOUNCE_PERIOD = 80;   // ticks per full bob cycle
      private static final float BOUNCE_AMP_ENTITY = 0.15f;
      private static final float BOUNCE_AMP_HEART = 0.08f;
      private static final float BOUNCE_AMP_BLOCK = 0.15f;
      private static final int ROT_PERIOD_ENTITY = 200;  // ticks per full CW rotation
      private static final int ROT_PERIOD_HEART = 100;  // ticks per full CCW rotation
      private static final int ROT_PERIOD_BLOCK = 200;
      private static final int INTERP_DURATION = 3;    // client interpolation ticks
      private static final int SHAKE_DURATION = 30;   // ticks of shake after taking damage
      private static final float SHAKE_MAX = 0.15f;
      private static final int WATCHER_CHECK_INTERVAL = 40;
      
      private final CreakingHeartEntity entity;
      private final ElementHolder packHolder;
      private final ElementHolder noPackHolder;
      private final ItemDisplayElement entityModelElement;
      private final ItemDisplayElement heartElement;
      private final BlockDisplayElement blockElement;
      private int animTick = 0;
      private int shakeTicks = 0;
      private String lastState = "full";
      
      DisplayManager(CreakingHeartEntity entity){
         this.entity = entity;
         float entityScale = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE) * 1.25f;
         float baseY = entity.getBbHeight() / 2.0f;
         
         this.packHolder = new PackFilterHolder();
         
         ItemStack entityModelStack = new ItemStack(Items.CLAY_BALL);
         entityModelStack.set(DataComponents.ITEM_MODEL, archetypesId("entity/creaking_heart_entity"));
         this.entityModelElement = new ItemDisplayElement(entityModelStack);
         this.entityModelElement.setItemDisplayContext(ItemDisplayContext.FIXED);
         this.entityModelElement.setViewRange(4.0f);
         this.entityModelElement.setBrightness(Brightness.FULL_BRIGHT);
         this.entityModelElement.setInterpolationDuration(INTERP_DURATION);
         this.entityModelElement.setScale(new Vector3f(entityScale, entityScale, entityScale));
         this.entityModelElement.setTranslation(new Vector3f(0, baseY, 0));
         
         ItemStack heartStack = new ItemStack(Items.CLAY_BALL);
         heartStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(new ArrayList<>(), new ArrayList<>(), List.of("on"), new ArrayList<>()));
         heartStack.set(DataComponents.ITEM_MODEL, archetypesId("creaking_heart"));
         this.heartElement = new ItemDisplayElement(heartStack);
         this.heartElement.setItemDisplayContext(ItemDisplayContext.FIXED);
         this.heartElement.setViewRange(4.0f);
         this.heartElement.setBrightness(Brightness.FULL_BRIGHT);
         this.heartElement.setInterpolationDuration(INTERP_DURATION);
         this.heartElement.setScale(new Vector3f(entityScale, entityScale, entityScale).mul(0.75f));
         this.heartElement.setTranslation(new Vector3f(0, baseY, 0));
         
         this.packHolder.addElement(entityModelElement);
         this.packHolder.addElement(heartElement);
         
         this.noPackHolder = new NoPackFilterHolder();
         
         BlockState heartBlockState = Blocks.CREAKING_HEART.defaultBlockState()
               .setValue(BlockStateProperties.CREAKING_HEART_STATE, CreakingHeartState.AWAKE);
         this.blockElement = new BlockDisplayElement(heartBlockState);
         this.blockElement.setViewRange(4.0f);
         this.blockElement.setBrightness(Brightness.FULL_BRIGHT);
         this.blockElement.setInterpolationDuration(INTERP_DURATION);
         this.blockElement.setScale(new Vector3f(entityScale, entityScale, entityScale).mul(0.75f));
         this.blockElement.setTranslation(new Vector3f(-entityScale * 0.5f, baseY - entityScale * 0.5f, -entityScale * 0.5f));
         
         this.noPackHolder.addElement(blockElement);
         
         EntityAttachment.ofTicking(packHolder, entity);
         EntityAttachment.ofTicking(noPackHolder, entity);
      }
      
      void tick(){
         animTick++;
         if(shakeTicks > 0) shakeTicks--;
         updateAnimations();
         if(animTick % WATCHER_CHECK_INTERVAL == 0) checkWatcherPackStatus();
      }
      
      private void checkWatcherPackStatus(){
         for(ServerGamePacketListenerImpl conn : new ArrayList<>(packHolder.getWatchingPlayers())){
            if(!PolymerResourcePackUtils.hasMainPack(conn.getPlayer())){
               packHolder.stopWatching(conn.getPlayer());
            }
         }
         for(ServerGamePacketListenerImpl conn : new ArrayList<>(noPackHolder.getWatchingPlayers())){
            if(PolymerResourcePackUtils.hasMainPack(conn.getPlayer())){
               noPackHolder.stopWatching(conn.getPlayer());
            }
         }
      }
      
      private void updateAnimations(){
         float entityScale = CONFIG.getFloat(ArchetypeRegistry.CREAKING_HEART_SCALE);
         float baseY = entity.getBbHeight() / 2.0f;
         float bouncePhase = (float) (animTick * Math.PI * 2.0 / BOUNCE_PERIOD);
         
         float sx = 0, sy = 0, sz = 0;
         if(shakeTicks > 0){
            float p = (float) shakeTicks / SHAKE_DURATION;
            sx = (entity.random.nextFloat() * 2 - 1) * SHAKE_MAX * p;
            sy = (entity.random.nextFloat() * 2 - 1) * SHAKE_MAX * p;
            sz = (entity.random.nextFloat() * 2 - 1) * SHAKE_MAX * p;
         }
         
         float entityBounceY = Mth.sin(bouncePhase) * BOUNCE_AMP_ENTITY;
         float entityRotAngle = (float) (animTick * Math.PI * 2.0 / ROT_PERIOD_ENTITY);
         entityModelElement.setTranslation(new Vector3f(sx, baseY + entityBounceY + sy, sz));
         entityModelElement.setLeftRotation(new Quaternionf().rotateY(entityRotAngle));
         entityModelElement.startInterpolation();
         
         float heartBounceY = Mth.sin(bouncePhase) * BOUNCE_AMP_HEART;
         float heartRotAngle = -(float) (animTick * Math.PI * 2.0 / ROT_PERIOD_HEART);
         heartElement.setTranslation(new Vector3f(sx * 0.75f, baseY + heartBounceY + sy * 0.75f, sz * 0.75f));
         heartElement.setLeftRotation(new Quaternionf().rotateY(heartRotAngle));
         heartElement.startInterpolation();
         
          // ---- block display: normal bounce + slow CW Y-rotation, kept centred ----
          // A 1×1×1 block model spans (0,0,0)→(1,1,1) in model space.
          // After scale(s) and rotateY(a), the block centre (0.5s, 0.5s, 0.5s) moves to:
          //   x' = 0.5s·cos(a) + 0.5s·sin(a),  z' = -0.5s·sin(a) + 0.5s·cos(a)
          // To keep the centre at world (0, baseY, 0) we set:
          //   tx = -x' = -0.5s·(cos(a) + sin(a)),  tz = -z' = 0.5s·(sin(a) - cos(a)),  ty = baseY - 0.5s
          float blockBounceY = Mth.sin(bouncePhase) * BOUNCE_AMP_BLOCK;
          float blockRotAngle = (float) (animTick * Math.PI * 2.0 / ROT_PERIOD_BLOCK);
          float cosA = Mth.cos(blockRotAngle);
          float sinA = Mth.sin(blockRotAngle);
          float blockTX = -0.5f * entityScale * (cosA + sinA) + sx;
          float blockTZ = 0.5f * entityScale * (sinA - cosA) + sz;
          float blockTY = baseY - entityScale * 0.5f + blockBounceY + sy;
          blockElement.setTranslation(new Vector3f(blockTX, blockTY, blockTZ));
          blockElement.setLeftRotation(new Quaternionf().rotateY(blockRotAngle));
          blockElement.startInterpolation();
         
         entityScale *= 1.25f;
         this.entityModelElement.setScale(new Vector3f(entityScale, entityScale, entityScale));
         this.heartElement.setScale(new Vector3f(entityScale, entityScale, entityScale).mul(0.75f));
         ItemStack stack = this.entityModelElement.getItem();
         float percentage = entity.getHealth() / entity.getMaxHealth();
         String str = "full";
         if(percentage < 0.25f){
            str = "damaged";
         }else if(percentage < 0.5f){
            str = "weakened";
         }
         if(!lastState.equals(str)){
            this.lastState = str;
            this.entityModelElement.setItem(ItemStack.EMPTY);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(new ArrayList<>(), new ArrayList<>(), List.of(str), new ArrayList<>()));
            this.entityModelElement.setItem(stack);
            float particleRange = entityScale * 0.5f;
            ((ServerLevel) this.entity.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, Blocks.CREAKING_HEART.defaultBlockState()),
                  this.entity.getX(), this.entity.getY() + baseY, this.entity.getZ(), 50, particleRange, particleRange, particleRange, 1);
         }
      }
      
      void triggerShake(){
         this.shakeTicks = SHAKE_DURATION;
      }
      
      void destroy(){
         packHolder.destroy();
         noPackHolder.destroy();
      }
      
      public void setInvisible(){
         this.blockElement.setInvisible(true);
         this.heartElement.setInvisible(true);
         this.entityModelElement.setInvisible(true);
      }
   }
}
