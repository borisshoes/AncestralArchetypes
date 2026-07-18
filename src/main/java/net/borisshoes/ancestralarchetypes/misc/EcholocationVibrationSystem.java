package net.borisshoes.ancestralarchetypes.misc;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.events.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.TeamColor;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EcholocationVibrationSystem implements VibrationSystem {
   
   private static final int MAX_PENDING_VIBRATIONS = 512;
   private static final int MAX_TRAVEL_TIME = 30;
   private static final int GLOW_SWEEP_INTERVAL = 1200;
   private static final double SOUND_ENTITY_GLOW_RADIUS = 1.0;
   private static final Map<UUID, EcholocationVibrationSystem> ACTIVE_SYSTEMS = new ConcurrentHashMap<>();
   private static long lastGlowSweepTick = Long.MIN_VALUE;
   private final UUID playerId;
   private final VibrationSystem.User vibrationUser;
   private final VibrationSystem.Data vibrationData;
   private final List<PendingVibration> pendingVibrations = new ArrayList<>();
   private ServerPlayer player;
   private long particleDecisionTick = Long.MIN_VALUE;
   private boolean particleAllowedThisTick = false;
   
   public EcholocationVibrationSystem(ServerPlayer player){
      this.player = player;
      this.playerId = player.getUUID();
      this.vibrationUser = new EcholocationVibrationSystem.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      ACTIVE_SYSTEMS.put(this.playerId, this);
   }
   
   public void unregister(){
      ACTIVE_SYSTEMS.remove(this.playerId, this);
   }
   
   /**
    * Updates the stored player reference and immediately re-registers this system in ACTIVE_SYSTEMS.
    * Call this on player relog so the game-event listener doesn't hold a stale disconnected player
    * reference until the next server tick would otherwise fix it via {@link #tick}.
    */
   public void updatePlayer(ServerPlayer player){
      this.player = player;
      ACTIVE_SYSTEMS.put(this.playerId, this);
   }
   
   public void tick(ServerPlayer player){
      if(this.player != player){
         this.player = player;
         ACTIVE_SYSTEMS.put(this.playerId, this);
      }
      ServerLevel serverLevel = this.player.level();
      
      Iterator<PendingVibration> it = this.pendingVibrations.iterator();
      while(it.hasNext()){
         PendingVibration pending = it.next();
         if(--pending.travelTime <= 0){
            it.remove();
            try{
               this.vibrationUser.onReceiveVibration(serverLevel, pending.originPos, pending.event, pending.sourceEntity, pending.projectileOwner, pending.receivingDistance);
            }catch(Exception e){
               AncestralArchetypes.log(2, "Echolocation failed to process a vibration: " + e);
            }
         }
      }
      
      long gameTime = serverLevel.getGameTime();
      if(gameTime - lastGlowSweepTick >= GLOW_SWEEP_INTERVAL){
         lastGlowSweepTick = gameTime;
         pruneStaleGlows();
      }
   }
   
   public static void dispatchGameEvent(final ServerLevel level, final Holder<GameEvent> event, final Vec3 pos, final GameEvent.Context context){
      if(ACTIVE_SYSTEMS.isEmpty()) return;
      Iterator<EcholocationVibrationSystem> it = ACTIVE_SYSTEMS.values().iterator();
      while(it.hasNext()){
         EcholocationVibrationSystem system = it.next();
         ServerPlayer player = system.player;
         if(player == null || player.hasDisconnected()){
            it.remove();
            continue;
         }
         if(player.level() != level) continue;
         system.handleGameEvent(level, event, context, pos);
      }
   }
   
   /**
    * Thread-safe check (backed by the concurrent registry) for whether a player currently has an active echolocation system.
    */
   public static boolean isActive(UUID playerId){
      return ACTIVE_SYSTEMS.containsKey(playerId);
   }
   
   private boolean handleGameEvent(final ServerLevel level, final Holder<GameEvent> event, final GameEvent.Context context, final Vec3 sourcePosition){
      VibrationSystem.User user = this.vibrationUser;
      
      if(!user.isValidVibration(event, context)) return false;
      
      Optional<Vec3> listenerPos = user.getPositionSource().getPosition(level);
      if(listenerPos.isEmpty()) return false;
      Vec3 destination = listenerPos.get();
      
      double range = AncestralArchetypes.CONFIG.getDouble(ArchetypeRegistry.ECHOLOCATION_RANGE);
      if(sourcePosition.distanceToSqr(destination) > range * range) return false;
      
      BlockPos originPos = BlockPos.containing(sourcePosition);
      if(!user.canReceiveVibration(level, originPos, event, context)) return false;
      if(isOccluded(level, sourcePosition, destination)) return false;
      
      Entity sourceEntity = context.sourceEntity();
      Entity projectileOwner = sourceEntity instanceof Projectile projectile ? projectile.getOwner() : null;
      BlockPos destinationPos = BlockPos.containing(destination);
      float receivingDistance = (float) Math.sqrt(originPos.distSqr(destinationPos));
      int travelTime = Math.max(1, user.calculateTravelTimeInTicks((float) sourcePosition.distanceTo(destination)));
      
      // If the queue is full, evict the least-imminent vibration to make room rather than dropping this new one
      if(this.pendingVibrations.size() >= MAX_PENDING_VIBRATIONS){
         PendingVibration furthest = null;
         for(PendingVibration pv : this.pendingVibrations){
            if(furthest == null || pv.travelTime > furthest.travelTime) furthest = pv;
         }
         if(furthest != null && furthest.travelTime > travelTime){
            this.pendingVibrations.remove(furthest);
         }else{
            return false;
         }
      }
      
      this.pendingVibrations.add(new PendingVibration(event, originPos, sourceEntity, projectileOwner, receivingDistance, travelTime));
      if(sourcePosition.distanceTo(destination) > 4) maybeSpawnTravelParticle(level, sourcePosition, travelTime);
      return true;
   }
   
   private void maybeSpawnTravelParticle(final ServerLevel level, final Vec3 source, final int travelTime){
      long gameTime = level.getGameTime();
      if(this.particleDecisionTick != gameTime){
         this.particleDecisionTick = gameTime;
         this.particleAllowedThisTick = true;
      }
      if(!this.particleAllowedThisTick) return;
      this.particleAllowedThisTick = false; // only one particle per tick
      
      VibrationParticleOption particle = new VibrationParticleOption(this.vibrationUser.getPositionSource(), travelTime);
      level.sendParticles(particle, true, true, source.x, source.y, source.z, 1, 0.0, 0.0, 0.0, 0.0);
   }
   
   @Override
   public VibrationSystem.Data getVibrationData(){
      return this.vibrationData;
   }
   
   @Override
   public VibrationSystem.User getVibrationUser(){
      return this.vibrationUser;
   }
   
   private static final class PendingVibration {
      private final Holder<GameEvent> event;
      private final BlockPos originPos;
      @Nullable
      private final Entity sourceEntity;
      @Nullable
      private final Entity projectileOwner;
      private final float receivingDistance;
      private int travelTime;
      
      private PendingVibration(Holder<GameEvent> event, BlockPos originPos, @Nullable Entity sourceEntity, @Nullable Entity projectileOwner, float receivingDistance, int travelTime){
         this.event = event;
         this.originPos = originPos;
         this.sourceEntity = sourceEntity;
         this.projectileOwner = projectileOwner;
         this.receivingDistance = receivingDistance;
         this.travelTime = travelTime;
      }
   }
   
   /**
    * Reimplementation of the private {@code VibrationSystem.Listener#isOccluded} occlusion check.
    */
   private static boolean isOccluded(final Level level, final Vec3 origin, final Vec3 dest){
      Vec3 from = new Vec3(Mth.floor(origin.x) + 0.5, Mth.floor(origin.y) + 0.5, Mth.floor(origin.z) + 0.5);
      Vec3 to = new Vec3(Mth.floor(dest.x) + 0.5, Mth.floor(dest.y) + 0.5, Mth.floor(dest.z) + 0.5);
      
      for(Direction direction : Direction.values()){
         Vec3 nudgedSource = from.relative(direction, 1.0E-5F);
         if(level.isBlockInLine(new ClipBlockStateContext(nudgedSource, to, state -> state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType() != HitResult.Type.BLOCK){
            return false;
         }
      }
      return true;
   }
   
   private class VibrationUser implements VibrationSystem.User {
      
      private VibrationUser(){
         super();
      }
      
      @Override
      public int getListenerRadius(){
         return AncestralArchetypes.CONFIG.getInt(ArchetypeRegistry.ECHOLOCATION_RANGE);
      }
      
      @Override
      public PositionSource getPositionSource(){
         ServerPlayer p = EcholocationVibrationSystem.this.player;
         return new EntityPositionSource(p, p.getEyeHeight() / 2.0f);
      }
      
      @Override
      public int calculateTravelTimeInTicks(final float distanceToDestination){
         return Math.min(MAX_TRAVEL_TIME, Mth.floor(distanceToDestination));
      }
      
      @Override
      public TagKey<GameEvent> getListenableEvents(){
         return GameEventTags.WARDEN_CAN_LISTEN;
      }
      
      @Override
      public boolean canTriggerAvoidVibration(){
         return true;
      }
      
      @Override
      public boolean canReceiveVibration(final ServerLevel level, final BlockPos pos, final Holder<GameEvent> event, final GameEvent.Context context){
         if(EcholocationVibrationSystem.this.player.isDeadOrDying()) return false;
         if(!level.getWorldBorder().isWithinBounds(pos)) return false;
         Entity sourceEntity = context.sourceEntity();
         if(sourceEntity instanceof ServerPlayer sourcePlayer){
            PlayerArchetypeData profile = AncestralArchetypes.profile(sourcePlayer);
            if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT) || profile.getMetamorph() == MetamorphTypes.WOOL){
               return false;
            }
         }
         return true;
      }
      
      @Override
      public void onReceiveVibration(
            final ServerLevel level,
            final BlockPos pos,
            final Holder<GameEvent> event,
            @Nullable final Entity sourceEntity,
            @Nullable final Entity projectileOwner,
            final float receivingDistance
      ){
         if(!EcholocationVibrationSystem.this.player.isDeadOrDying()){
            level.broadcastEntityEvent(EcholocationVibrationSystem.this.player, (byte) 61);
            
            if(sourceEntity != null && !player.getUUID().equals(sourceEntity.getUUID())){
               glowEntity(EcholocationVibrationSystem.this.player, sourceEntity);
            }
            
            if(event.equals(GameEvent.STEP) && sourceEntity != null){
               BlockPos standing = sourceEntity.getOnPos();
               if(!standing.equals(pos) && !level.getBlockState(standing).isAir()){
                  glowBlock(level, standing, EcholocationVibrationSystem.this.player);
               }
            }
            
            if(event.equals(GameEvent.BLOCK_DESTROY)){
               glowDestroyedBlock(level, pos, EcholocationVibrationSystem.this.player);
            }else if(!level.getBlockState(pos).isAir()){
               glowBlock(level, pos, EcholocationVibrationSystem.this.player);
            }
         }
      }
   }
   
   // Keyed by a hash of (dimension, pos, block) so all glow requests for the same block collapse onto a single
   // shared virtual entity. Collisions across different blocks are acceptable/intended for this dedup.
   private static final HashMap<Integer, GlowedBlock> GLOWED_BLOCKS = new HashMap<>();
   
   private static void pruneStaleGlows(){
      if(GLOWED_BLOCKS.isEmpty()) return;
      Iterator<GlowedBlock> it = GLOWED_BLOCKS.values().iterator();
      while(it.hasNext()){
         GlowedBlock gb = it.next();
         HolderAttachment attachment = gb.holder.getAttachment();
         boolean orphaned = attachment == null || attachment.isRemoved()
               || !gb.level.getChunkSource().hasChunk(gb.pos.getX() >> 4, gb.pos.getZ() >> 4);
         if(orphaned){
            gb.holder.setAttachment(null);
            gb.holder.destroy();
            it.remove();
         }
      }
   }
   
   public static void glowBlock(ServerLevel level, BlockPos pos, ServerPlayer player){
      BlockState state = level.getBlockState(pos);
      // Canonicalize two-part blocks (beds, doors, double chests, ...) so hitting either half maps to the
      // same GlowedBlock. The half with the lower packed position always wins, deterministically.
      BlockPos partner = getPartnerPos(state, pos);
      if(partner != null && partner.asLong() < pos.asLong()){
         pos = partner;
         state = level.getBlockState(pos);
      }
      GlowedBlock existing = GLOWED_BLOCKS.get(GlowedBlock.getHash(level, pos, state));
      if(existing != null){
         existing.addPlayer(player);
      }else{
         GlowedBlock newBlock = new GlowedBlock(level, pos, false);
         newBlock.addPlayer(player);
         GLOWED_BLOCKS.put(newBlock.hashCode(), newBlock);
      }
   }
   
   /**
    * Marks the location of a just-destroyed block (now air) with a billboarded, glowing pickaxe item
    * display centred in the block. Reuses the same dedup / player-tracking / timeout machinery as
    * {@link #glowBlock}, but the marker isn't tied to a block being present.
    */
   public static void glowDestroyedBlock(ServerLevel level, BlockPos pos, ServerPlayer player){
      GlowedBlock existing = GLOWED_BLOCKS.get(GlowedBlock.getHash(level, pos, level.getBlockState(pos)));
      if(existing != null){
         existing.addPlayer(player);
      }else{
         GlowedBlock newBlock = new GlowedBlock(level, pos, true);
         newBlock.addPlayer(player);
         GLOWED_BLOCKS.put(newBlock.hashCode(), newBlock);
      }
   }
   
   private static void glowEntity(ServerPlayer player, @Nullable Entity entity){
      if(entity == null || player.getUUID().equals(entity.getUUID())) return;
      if(entity instanceof ServerPlayer sourcePlayer){
         PlayerArchetypeData profile = AncestralArchetypes.profile(sourcePlayer);
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT) || profile.getMetamorph() == MetamorphTypes.WOOL){
            return;
         }
      }
      ArchetypeUtils.addGlow(player, entity, TeamColor.DARK_AQUA);
      Event.addEvent(new EcholocationEntityGlowEvent(entity, player));
   }
   
   /**
    * Re-applies entity glow packets for every active {@link EcholocationEntityGlowEvent}.
    *
    * <p>This is called from {@link net.borisshoes.ancestralarchetypes.callbacks.TickCallback} at
    * {@code ServerTickEvents.END_SERVER_TICK} — i.e. <em>after</em> the entity tracker has already flushed its
    * per-player data updates for this tick. Because the tracker can send a FLAGS value that does not include the
    * glowing bit (whenever any flag changes on the entity), our glow packet needs to be re-sent every couple of
    * ticks so the client's final state for the tick always ends with the glow active.
    */
   public static void refreshEntityGlows(){
      List<EcholocationEntityGlowEvent> events = Event.getEventsOfType(EcholocationEntityGlowEvent.class);
      if(events.isEmpty()) return;
      for(EcholocationEntityGlowEvent event : events){
         if(event.entity == null || !event.entity.isAlive() || event.entity.isRemoved()) continue;
         if(event.player == null || event.player.hasDisconnected()) continue;
         ArchetypeUtils.addGlow(event.player, event.entity, TeamColor.DARK_AQUA);
      }
   }
   
   public static void handleSoundAt(ServerPlayer player, double x, double y, double z){
      if(!isActive(player.getUUID()) || player.isDeadOrDying() || player.hasDisconnected()) return;
      ServerLevel level = player.level();
      
      Vec3 origin = new Vec3(x, y, z);
      double range = AncestralArchetypes.CONFIG.getDouble(ArchetypeRegistry.ECHOLOCATION_RANGE);
      if(player.getEyePosition().distanceToSqr(origin) > range * range) return;
      
      double lwRadius = SOUND_ENTITY_GLOW_RADIUS + 0.15;
      boolean fromLightweight = level.getPlayers(p ->
            !p.getUUID().equals(player.getUUID()) &&
                  p.position().closerThan(origin, lwRadius) &&
                  (AncestralArchetypes.profile(p).hasAbility(ArchetypeRegistry.LIGHTWEIGHT) || AncestralArchetypes.profile(p).getMetamorph() == MetamorphTypes.WOOL)
      ).stream().findAny().isPresent();
      if(fromLightweight) return;
      
      BlockPos pos = BlockPos.containing(origin);
      if(!level.getBlockState(pos).isAir()){
         glowBlock(level, pos, player);
      }else if(!level.getBlockState(pos.below()).isAir()){
         glowBlock(level, pos.below(), player);
      }
      
      double r = SOUND_ENTITY_GLOW_RADIUS;
      for(Entity entity : level.getEntities(player, new AABB(origin.subtract(r, r, r), origin.add(r, r, r)))){
         glowEntity(player, entity);
      }
   }
   
   public static void handleEntitySound(ServerPlayer player, int entityId){
      ServerLevel level = player.level();
      Entity entity = level.getEntity(entityId);
      if(entity == null) return;
      if(entity instanceof ServerPlayer sourcePlayer){
         PlayerArchetypeData profile = AncestralArchetypes.profile(sourcePlayer);
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT) || profile.getMetamorph() == MetamorphTypes.WOOL){
            return;
         }
      }
      handleSoundAt(player, entity.getX(), entity.getY(), entity.getZ());
   }
   
   /**
    * Returns the position of the connected second half of a two-part block, or {@code null} if the block
    * is not multi-part. Handles the {@link DoubleBlockHalf} blocks (doors, tall plants), beds, and double
    * chests via their vanilla connection helpers, so it works generally for any block using those schemes.
    */
   @Nullable
   private static BlockPos getPartnerPos(BlockState state, BlockPos pos){
      if(state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){
         return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
      }
      if(state.getBlock() instanceof BedBlock){
         return pos.relative(BedBlock.getConnectedDirection(state));
      }
      if(state.getBlock() instanceof ChestBlock && state.hasProperty(BlockStateProperties.CHEST_TYPE)
            && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE){
         return pos.relative(ChestBlock.getConnectedDirection(state));
      }
      return null;
   }
   
   private static class GlowedBlock {
      private final ServerLevel level;
      private final BlockPos pos;
      private final List<DisplayPart> parts = new ArrayList<>();
      private BlockState state;
      private GlowedBlockElementHolder holder;
      
      private GlowedBlock(ServerLevel level, BlockPos pos, boolean destroyedMarker){
         this.level = level;
         this.pos = pos;
         if(destroyedMarker){
            initializeMarker();
         }else{
            initialize();
         }
      }
      
      private void initialize(){
         this.state = level.getBlockState(pos);
         this.holder = new GlowedBlockElementHolder(this);
         
         addDisplay(pos);
         
         // Glow the connected half of two-part blocks (beds, doors, double chests, ...) too, as a second
         // display element offset to the partner's position within the same holder.
         BlockPos partner = getPartnerPos(this.state, pos);
         if(partner != null && !level.getBlockState(partner).isAir()){
            addDisplay(partner);
         }
         
         BlockBoundAttachment attachment = new BlockBoundAttachment(this.holder, level.getChunkAt(pos), this.state, pos, new Vec3(pos), true);
      }
      
      private void initializeMarker(){
         this.state = level.getBlockState(pos); // air, since the block was just destroyed
         this.holder = new GlowedBlockElementHolder(this);
         
         addPickaxeMarker();
         
         BlockBoundAttachment attachment = new BlockBoundAttachment(this.holder, level.getChunkAt(pos), this.state, pos, new Vec3(pos), true);
      }
      
      /**
       * Creates a glowing block display element for the (absolute) block position, offset within the holder.
       */
      private void addDisplay(BlockPos partPos){
         BlockDisplayElement blockDisplay = new BlockDisplayElement(toDisplayState(level.getBlockState(partPos)));
         blockDisplay.setGlowing(true);
         blockDisplay.setGlowColorOverride(TextColor.DARK_AQUA.getValue());
         blockDisplay.setViewRange(256.0f);
         blockDisplay.setDisplaySize(1024, 1024);
         blockDisplay.setBrightness(Brightness.FULL_BRIGHT);
         // 99.99 % scale to prevent Z-fighting with nearby geometry.
         blockDisplay.setScale(new Vector3f(0.9999f, 0.9999f, 0.9999f));
         blockDisplay.setOffset(new Vec3(
               partPos.getX() - pos.getX() + 0.00005,
               partPos.getY() - pos.getY() + 0.00005,
               partPos.getZ() - pos.getZ() + 0.00005));
         this.holder.addElement(blockDisplay);
         this.parts.add(new DisplayPart(partPos, blockDisplay, true));
      }
      
      /**
       * Creates a billboarded glowing pickaxe marker centred in the block (used for destroyed blocks).
       */
      private void addPickaxeMarker(){
         ItemDisplayElement itemDisplay = new ItemDisplayElement(Items.DIAMOND_PICKAXE);
         itemDisplay.setItemDisplayContext(ItemDisplayContext.FIXED);
         itemDisplay.setBillboardMode(Display.BillboardConstraints.CENTER);
         itemDisplay.setGlowing(true);
         itemDisplay.setGlowColorOverride(TextColor.DARK_AQUA.getValue());
         itemDisplay.setViewRange(256.0f);
         itemDisplay.setBrightness(Brightness.FULL_BRIGHT);
         itemDisplay.setScale(new Vector3f(0.75f, 0.75f, 0.75f));
         itemDisplay.setOffset(new Vec3(0.5, 0.5, 0.5)); // centre of the block (holder sits at the block corner)
         this.holder.addElement(itemDisplay);
         this.parts.add(new DisplayPart(pos, itemDisplay, false));
      }
      
      /**
       * Re-syncs every block-bound display element to its live world block state. Doors/trapdoors opening and
       * closing, etc. are reflected immediately. Any block-bound part whose block has been broken (now air) is
       * removed. Non-block-bound parts (e.g. the destroyed-block pickaxe marker) are left untouched.
       *
       * @return true if all parts are now gone (every glowed element was removed).
       */
      private boolean updateStates(){
         Iterator<DisplayPart> it = this.parts.iterator();
         while(it.hasNext()){
            DisplayPart part = it.next();
            if(!part.blockBound) continue;
            BlockState actual = level.getBlockState(part.partPos);
            if(actual.isAir()){
               this.holder.removeElement(part.element);
               it.remove();
               continue;
            }
            BlockState displayState = toDisplayState(actual);
            if(part.element instanceof BlockDisplayElement blockElement && !displayState.equals(blockElement.getBlockState())){
               blockElement.setBlockState(displayState);
            }
         }
         return this.parts.isEmpty();
      }
      
      /**
       * Display entities can't render fluid block states in this version, so fluids are substituted with a
       * translucent full-cube block that does render (and therefore can glow). Waterlogged solid blocks keep
       * their own model since their block isn't a {@link LiquidBlock}.
       */
      private static BlockState toDisplayState(BlockState actual){
         if(actual.getBlock() instanceof LiquidBlock){
            boolean lava = actual.getFluidState().is(FluidTags.LAVA);
            return (lava ? Blocks.STAINED_GLASS.orange() : Blocks.STAINED_GLASS.blue()).defaultBlockState();
         }
         return actual;
      }
      
      private void addPlayer(ServerPlayer player){
         this.holder.addPlayer(player);
      }
      
      public static int getHash(ServerLevel level, BlockPos pos, BlockState state){
         return Objects.hash(level.dimension().toString().hashCode(), pos.hashCode(), state.getBlock().toString().hashCode());
      }
      
      @Override
      public int hashCode(){
         return getHash(level, pos, state);
      }
      
      /**
       * A single glowing display element. Block-bound parts track a world block; markers are standalone.
       */
      private static final class DisplayPart {
         private final BlockPos partPos;
         private final DisplayElement element;
         private final boolean blockBound;
         
         private DisplayPart(BlockPos partPos, DisplayElement element, boolean blockBound){
            this.partPos = partPos;
            this.element = element;
            this.blockBound = blockBound;
         }
      }
   }
   
   private static class GlowedBlockElementHolder extends ElementHolder {
      private final HashMap<ServerPlayer, Integer> players;
      private final ResourceKey<Level> levelKey;
      private final GlowedBlock glowedBlock;
      
      private GlowedBlockElementHolder(GlowedBlock glowedBlock){
         this.players = new HashMap<>();
         this.glowedBlock = glowedBlock;
         this.levelKey = glowedBlock.level.dimension();
      }
      
      void addPlayer(ServerPlayer player){
         players.put(player, 60);
         startWatching(player);
         if(getAttachment() != null) getAttachment().startWatching(player);
      }
      
      void removePlayer(ServerPlayer player){
         players.remove(player);
         stopWatching(player);
         if(getAttachment() != null) getAttachment().stopWatching(player);
      }
      
      /**
       * The {@link BlockBoundAttachment} backing this holder is a {@link eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment}
       * which, via {@code updateCurrentlyTracking}, automatically force-starts watching for EVERY player tracking the
       * chunk — including players without the echolocation ability. Gate visibility here so the glow entities are only
       * ever shown to players explicitly registered via {@link #addPlayer} (those who actually pinged this block).
       */
      @Override
      public boolean startWatching(ServerGamePacketListenerImpl player){
         if(!this.players.containsKey(player.getPlayer())) return false;
         return super.startWatching(player);
      }
      
      @Override
      protected void onTick(){
         super.onTick();
         
         // Keep each display in sync with the live world (doors/trapdoors opening, blocks broken, etc.).
         // If every glowed block has been destroyed, tear the whole holder down immediately.
         if(glowedBlock.updateStates()){
            setAttachment(null);
            destroy();
            GLOWED_BLOCKS.remove(glowedBlock.hashCode());
            return;
         }
         
         int maxTime = 0;
         List<ServerPlayer> toRemove = new ArrayList<>();
         for(ServerPlayer player : players.keySet()){
            int newTime = players.compute(player, (k, time) -> time - 1);
            if(newTime <= 0 || player.isDeadOrDying() || player.hasDisconnected() || !player.level().dimension().equals(this.levelKey))
               toRemove.add(player);
            if(newTime > maxTime) maxTime = newTime;
         }
         toRemove.forEach(this::removePlayer);
         
         if(maxTime == 0){
            setAttachment(null);
            destroy();
            GLOWED_BLOCKS.remove(glowedBlock.hashCode());
         }
      }
   }
}
