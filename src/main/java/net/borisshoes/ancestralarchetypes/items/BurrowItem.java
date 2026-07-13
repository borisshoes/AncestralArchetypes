package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.misc.TeleportIndicator;
import net.borisshoes.borislib.sequences.CameraKeyframe;
import net.borisshoes.borislib.sequences.CameraPath;
import net.borisshoes.borislib.sequences.IFrameSequence;
import net.borisshoes.borislib.sequences.InterpolationType;
import net.borisshoes.borislib.sequences.SequenceManager;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class BurrowItem extends DirectionalTeleportItem {

   public BurrowItem(Properties settings){
      super(ArchetypeRegistry.BURROW, "\uD83E\uDE8F", settings);
   }

   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.CLAY_BALL;
   }

   @Override
   protected double getMaxRange(){
      return CONFIG.getDouble(ArchetypeRegistry.BURROW_RANGE);
   }

   @Override
   protected int getCooldownTicks(){
      return CONFIG.getInt(ArchetypeRegistry.BURROW_COOLDOWN);
   }

   @Override
   protected ParticleOptions getIndicatorParticle(){
      return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
   }

   @Override
   protected int getGlowColor(){
      return 0x745123;
   }

   /** Burrow requires the player to be standing on solid ground. */
   @Override
   protected boolean canActivate(ServerPlayer player, ServerLevel world){
      return player.onGround();
   }

   @Override
   protected @Nullable Component getNotReadyMessage(){
      return Component.translatable("text.ancestralarchetypes.burrow_not_grounded").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
   }

   /**
    * The destination must be reachable via a continuous path of solid ground.
    */
   @Override
   protected boolean isValidDestination(ServerLevel world, ServerPlayer user, Vec3 spot){
      return isPathGrounded(world, user, spot);
   }

   /**
    * Samples points along the straight path from the player to the destination and
    * checks each sample has solid ground within a short drop below it.
    */
   private boolean isPathGrounded(ServerLevel world, ServerPlayer user, Vec3 destination){
      Vec3 origin = user.position();
      double distance = origin.distanceTo(destination);
      int steps = Math.max(2, (int)(distance / 1.5));
      for(int i = 0; i <= steps; i++){
         double t = (double) i / steps;
         Vec3 sample = new Vec3(
               origin.x + t * (destination.x - origin.x),
               origin.y + t * (destination.y - origin.y),
               origin.z + t * (destination.z - origin.z)
         );
         if(!hasSolidGroundNearby(world, sample)){
            return false;
         }
      }
      return true;
   }

   /** Returns true if there is a solid (opaque) block within 3 blocks below the given position. */
   private boolean hasSolidGroundNearby(ServerLevel world, Vec3 pos){
      for(double dy = 0.0; dy <= 3.0; dy += 0.5){
         BlockPos blockPos = BlockPos.containing(pos.x, pos.y - dy, pos.z);
         BlockState state = world.getBlockState(blockPos);
         if(state.isSolidRender()){
            return true;
         }
      }
      return false;
   }

   /**
    * Instead of teleporting instantly, starts a {@link BurrowSequence} that animates the
    * player sinking into the ground, travelling underground, and emerging at the destination.
    */
   @Override
   protected boolean performTeleport(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 cachedSpot = getCachedSpot(playerUUID);

      Vec3 destination = null;
      if(cachedSpot != null
            && isSpaceClearFor(user, world, cachedSpot)
            && hasGroundSupport(world, user, cachedSpot)
            && isValidDestination(world, user, cachedSpot)){
         destination = cachedSpot;
      }else{
         destination = findTeleportSpot(world, user);
      }

      if(destination == null) return false;

      TeleportIndicator.hide(user);
      removeCachedSpot(playerUUID);

      return SequenceManager.start(user, BurrowSequence.create(user, destination, getMaxRange()));
   }

   // ─────────────────────────────── inner sequence ──────────────────────────

   /**
    * Drives the burrow animation:
    * <ol>
    *   <li>20-tick sink phase  — player descends into the ground at origin.</li>
    *   <li>10–110-tick travel phase — player moves underground to the destination.</li>
    *   <li>20-tick rise phase  — player ascends from underground at destination.</li>
    * </ol>
    * Block-breaking particles burst at the sink and rise transitions.
    * Full damage+knockback immunity is active for the whole animation.
    */
   private static class BurrowSequence extends IFrameSequence {

      private static final int SINK_TICKS = 20;
      private static final int RISE_TICKS = 20;
      /** How many blocks below the surface the underground travel occurs. */
      private static final double UNDERGROUND_DEPTH = 2.0;

      private final Vec3 origin;
      private final Vec3 destination;
      private final int riseStartTick;

      private BurrowSequence(UUID playerUUID, Vec3 origin, Vec3 destination, int totalTicks, int travelTicks, CameraPath path){
         super(playerUUID, totalTicks, true, true, false, path);
         this.origin = origin;
         this.destination = destination;
         this.riseStartTick = SINK_TICKS + travelTicks;
      }

      static BurrowSequence create(ServerPlayer player, Vec3 destination, double maxRange){
         Vec3 origin = player.position();
         double distance = origin.distanceTo(destination);
         int travelTicks = (int) Mth.clamp(10.0 + (distance / maxRange) * 100.0, 10, 110);
         int totalTicks = SINK_TICKS + travelTicks + RISE_TICKS;

         double sinkEnd   = (double) SINK_TICKS / totalTicks;
         double travelEnd = (double) (SINK_TICKS + travelTicks) / totalTicks;
         float yaw   = player.getYRot();
         float pitch = player.getXRot();

         CameraPath path = CameraPath.builder()
               .add(CameraKeyframe.at(0.0)
                     .pos(origin)
                     .rot(yaw, pitch)
                     .build())
               .add(CameraKeyframe.at(sinkEnd)
                     .pos(origin.x, origin.y - UNDERGROUND_DEPTH, origin.z)
                     .rot(yaw, pitch)
                     .interp(InterpolationType.EASE_IN)
                     .build())
               .add(CameraKeyframe.at(travelEnd)
                     .pos(destination.x, destination.y - UNDERGROUND_DEPTH, destination.z)
                     .rot(yaw, pitch)
                     .interp(InterpolationType.LINEAR)
                     .build())
               .add(CameraKeyframe.at(1.0)
                     .pos(destination)
                     .rot(yaw, pitch)
                     .interp(InterpolationType.EASE_OUT)
                     .build())
               .build();

         return new BurrowSequence(player.getUUID(), origin, destination, totalTicks, travelTicks, path);
      }

      @Override
      public void onStart(ServerPlayer player){
         super.onStart(player);
         ServerLevel world = player.level();
         world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 1.0f, 1.0f);
         spawnBreakParticles(world, origin);
      }

      @Override
      public void onTick(ServerPlayer player, int tick){
         super.onTick(player, tick);
         // Spawn emerging particles and sound at the transition into the rise phase.
         if(tick == riseStartTick){
            ServerLevel world = player.level();
            world.playSound(null, destination.x, destination.y, destination.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnBreakParticles(world, destination);
         }
         if(tick > SINK_TICKS && tick < riseStartTick){
            player.setPose(Pose.SWIMMING);
         }else{
            player.setPose(Pose.STANDING);
         }
      }

      private static void spawnBreakParticles(ServerLevel world, Vec3 feet){
         // Block-crumble particles from the surface block being broken through.
         BlockPos below = BlockPos.containing(feet).below();
         BlockState state = world.getBlockState(below);
         if(!state.isAir()){
            BlockParticleOption blockParticles = new BlockParticleOption(ParticleTypes.BLOCK, state);
            world.sendParticles(blockParticles,
                  feet.x, feet.y + 0.1, feet.z,
                  30, 0.4, 0.15, 0.4, 0.18);
         }
         // Extra sculk soul particles for visual flair.
         world.sendParticles(ParticleTypes.SCULK_SOUL,
               feet.x, feet.y + 0.6, feet.z,
               6, 0.3, 0.2, 0.3, 0.04);
      }
   }
}