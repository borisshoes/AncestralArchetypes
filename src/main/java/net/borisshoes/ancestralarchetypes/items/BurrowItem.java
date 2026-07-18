package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.misc.TeleportIndicator;
import net.borisshoes.borislib.sequences.*;
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

import java.util.*;

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
   
   @Override
   protected boolean canActivate(ServerPlayer player, ServerLevel world){
      return player.onGround();
   }
   
   @Override
   protected @Nullable Component getNotReadyMessage(){
      return Component.translatable("text.ancestralarchetypes.burrow_not_grounded").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
   }
   
   @Override
   protected boolean isValidDestination(ServerLevel world, ServerPlayer user, Vec3 spot){
      return isReachable(world, user, spot);
   }
   
   @Override
   protected boolean isValidCachedDestination(ServerLevel world, ServerPlayer user, Vec3 spot){
      return isReachable(world, user, spot);
   }
   
   @Override
   protected boolean performTeleport(ServerLevel world, ServerPlayer user){
      UUID uuid = user.getUUID();
      
      Vec3 destination = getCachedSpot(uuid);
      if(destination == null) destination = findTeleportSpot(world, user);
      if(destination == null) return false;
      
      List<Vec3> path = floodPath(world, user, destination);
      if(path == null) path = List.of(user.position(), destination);
      
      TeleportIndicator.hide(user);
      removeCachedSpot(uuid);
      
      return SequenceManager.start(user, BurrowSequence.create(user, destination, path, getMaxRange()));
   }
   
   private static final int SURFACE_STEP = 1;      // height change treated as a walkable step (slopes/stairs)
   private static final double RANGE_BUFFER = 2.0; // XZ leniency beyond max range
   private static final int FLOOD_NODE_CAP = 8000; // safety cap on explored surface nodes
   
   /**
    * Cached flood per player, keyed by origin block + world tick so it is computed once per search.
    */
   private final Map<UUID, FloodResult> floodCache = new HashMap<>();
   
   /**
    * @param cameFrom surface node -> parent node (origin maps to itself)
    */
   private record FloodResult(long originKey, long tick, Map<Long, Long> cameFrom) {
   }
   
   /**
    * Node key = packed block position of the solid block a player would stand on.
    */
   private static long node(int x, int surfaceY, int z){
      return BlockPos.asLong(x, surfaceY, z);
   }
   
   private static long goalNode(Vec3 spot){
      return node(Mth.floor(spot.x), Mth.floor(spot.y) - 1, Mth.floor(spot.z));
   }
   
   /**
    * Returns the flood for the player's current position, recomputing only when it moves or the tick changes.
    */
   private FloodResult getFlood(ServerLevel world, ServerPlayer user){
      BlockPos onPos = user.getOnPos();
      int ox = onPos.getX(), oz = onPos.getZ(), oy = onPos.getY();
      long originKey = node(ox, oy, oz);
      long tick = world.getServer().getTickCount();
      UUID uuid = user.getUUID();
      FloodResult cached = floodCache.get(uuid);
      if(cached != null && cached.originKey == originKey && cached.tick == tick) return cached;
      FloodResult fresh = computeFlood(world, ox, oy, oz, getMaxRange());
      floodCache.put(uuid, fresh);
      return fresh;
   }
   
   private boolean isReachable(ServerLevel world, ServerPlayer user, Vec3 spot){
      return getFlood(world, user).cameFrom.containsKey(goalNode(spot));
   }
   
   /**
    * Breadth-first flood over standable surfaces, moving only between columns that are
    * connected through solid material. Adjacent surfaces within {@link #SURFACE_STEP} always
    * connect (flat ground, slopes, staircases); larger drops/climbs connect only when a
    * continuous solid shaft/wall bridges them (pillars, cliffs, cave walls). This makes it
    * impossible to burrow across an air gap (e.g. off a floating platform or straight down
    * through the void), and the parent pointers give the exact route for the animation.
    */
   private static FloodResult computeFlood(ServerLevel world, int ox, int oy, int oz, double maxRange){
      Map<Long, Long> cameFrom = new HashMap<>();
      Map<Long, Boolean> solid = new HashMap<>();
      long tick = world.getServer().getTickCount();
      long start = node(ox, oy, oz);
      if(!isSolid(world, ox, oy, oz, solid)) return new FloodResult(start, tick, cameFrom);
      
      double rangeSq = (maxRange + RANGE_BUFFER) * (maxRange + RANGE_BUFFER);
      int band = (int) Math.ceil(maxRange) + 2;
      final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
      
      ArrayDeque<Long> queue = new ArrayDeque<>();
      cameFrom.put(start, start);
      queue.add(start);
      int nodes = 0;
      
      while(!queue.isEmpty() && nodes < FLOOD_NODE_CAP){
         long cur = queue.poll();
         nodes++;
         int cx = BlockPos.getX(cur), cy = BlockPos.getY(cur), cz = BlockPos.getZ(cur);
         
         for(int[] dir : dirs){
            int nx = cx + dir[0], nz = cz + dir[1];
            double ddx = (nx + 0.5) - (ox + 0.5), ddz = (nz + 0.5) - (oz + 0.5);
            if(ddx * ddx + ddz * ddz > rangeSq) continue;
            
            // Scan the neighbour column for standable ledges within the vertical band and
            // link every one that is solidly connected to the current surface.
            for(int ny = cy + band; ny >= cy - band; ny--){
               if(!isStandable(world, nx, ny, nz, solid)) continue;
               long nk = node(nx, ny, nz);
               if(cameFrom.containsKey(nk)) continue;
               if(!connected(world, cx, cy, cz, nx, ny, nz, solid)) continue;
               cameFrom.put(nk, cur);
               queue.add(nk);
            }
         }
      }
      return new FloodResult(start, tick, cameFrom);
   }
   
   /**
    * True if a player could travel between the two adjacent standable surfaces without
    * crossing air: equal-ish heights step freely, while a larger drop needs the higher
    * column solid down to the lower surface, and a larger climb needs the target column
    * solid up to the starting surface.
    */
   private static boolean connected(ServerLevel world, int ax, int ay, int az, int bx, int by, int bz, Map<Long, Boolean> solid){
      int d = ay - by;
      if(Math.abs(d) <= SURFACE_STEP) return true;
      if(d > 0){ // descend from A to lower B: need a solid shaft in A's column down to B's level
         return columnSolid(world, ax, az, by, ay - 1, solid);
      }
      // climb from A up to higher B: need a solid wall in B's column up to A's level
      return columnSolid(world, bx, bz, ay, by - 1, solid);
   }
   
   private static boolean columnSolid(ServerLevel world, int x, int z, int loY, int hiY, Map<Long, Boolean> solid){
      for(int y = loY; y <= hiY; y++){
         if(!isSolid(world, x, y, z, solid)) return false;
      }
      return true;
   }
   
   private static boolean isStandable(ServerLevel world, int x, int y, int z, Map<Long, Boolean> solid){
      return isSolid(world, x, y, z, solid) && !isSolid(world, x, y + 1, z, solid) && !isSolid(world, x, y + 2, z, solid);
   }
   
   private static boolean isSolid(ServerLevel world, int x, int y, int z, Map<Long, Boolean> cache){
      long key = BlockPos.asLong(x, y, z);
      Boolean c = cache.get(key);
      if(c != null) return c;
      boolean s = world.getBlockState(new BlockPos(x, y, z)).isSolidRender();
      cache.put(key, s);
      return s;
   }
   
   // ─── Path reconstruction ──────────────────────────────────────────────────
   
   /**
    * Rebuilds the burrow route to {@code dest} from the connectivity flood, adding a corner
    * waypoint at each large drop/climb so the animation hugs the solid (down-then-over /
    * over-then-up) instead of cutting a straight diagonal through the ground or open air.
    */
   @Nullable
   private List<Vec3> floodPath(ServerLevel world, ServerPlayer user, Vec3 dest){
      FloodResult flood = getFlood(world, user);
      long goal = goalNode(dest);
      if(!flood.cameFrom.containsKey(goal)) return null;
      
      List<Long> nodes = new ArrayList<>();
      long cur = goal;
      while(true){
         nodes.add(cur);
         long parent = flood.cameFrom.get(cur);
         if(parent == cur) break;
         cur = parent;
      }
      Collections.reverse(nodes);
      
      List<Vec3> path = new ArrayList<>();
      path.add(user.position());
      for(int i = 1; i < nodes.size(); i++){
         long p = nodes.get(i - 1), n = nodes.get(i);
         int px = BlockPos.getX(p), py = BlockPos.getY(p), pz = BlockPos.getZ(p);
         int x = BlockPos.getX(n), y = BlockPos.getY(n), z = BlockPos.getZ(n);
         if(py - y > SURFACE_STEP){ // dropped: descend the previous column first, then cross
            path.add(new Vec3(px + 0.5, y + 1.0, pz + 0.5));
         }else if(y - py > SURFACE_STEP){ // climbed: cross at the low level first, then rise
            path.add(new Vec3(x + 0.5, py + 1.0, z + 0.5));
         }
         path.add(new Vec3(x + 0.5, y + 1.0, z + 0.5));
      }
      if(path.size() > 1) path.set(path.size() - 1, dest);
      return path;
   }
   
   // ─── Inner sequence ──────────────────────────────────────────────────────
   
   /**
    * Drives the burrow animation along the discovered ground path:
    * 20-tick sink → variable-length underground travel → 20-tick rise.
    * The underground CameraPath keyframes follow the terrain contour (each waypoint
    * is shifted UNDERGROUND_DEPTH below its surface position).
    */
   private static class BurrowSequence extends IFrameSequence {
      
      private static final int SINK_TICKS = 20;
      private static final int RISE_TICKS = 20;
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
      
      static BurrowSequence create(ServerPlayer player, Vec3 dest, List<Vec3> waypoints, double maxRange){
         Vec3 origin = player.position();
         double dist = origin.distanceTo(dest);
         int travelTicks = (int) Mth.clamp(10.0 + (dist / maxRange) * 100.0, 10, 110);
         int total = SINK_TICKS + travelTicks + RISE_TICKS;
         
         CameraPath path = buildPath(origin, dest, waypoints, player.getYRot(), player.getXRot(), total, travelTicks);
         return new BurrowSequence(player.getUUID(), origin, dest, total, travelTicks, path);
      }
      
      /**
       * Builds a CameraPath with keyframes for sink, underground waypoints (distance-
       * proportional timing so movement speed feels constant), and rise.
       */
      private static CameraPath buildPath(Vec3 origin, Vec3 dest, List<Vec3> waypoints, float yaw, float pitch, int total, int travelTicks){
         double sinkEnd = (double) SINK_TICKS / total;
         double travelEnd = (double) (SINK_TICKS + travelTicks) / total;
         
         CameraPath.Builder builder = CameraPath.builder().add(CameraKeyframe.at(0.0).pos(origin).rot(yaw, pitch).build()).add(CameraKeyframe.at(sinkEnd).pos(origin.x, origin.y - UNDERGROUND_DEPTH, origin.z).rot(yaw, pitch).interp(InterpolationType.EASE_IN).build());
         
         // Add intermediate underground waypoints with distance-proportional timing.
         if(waypoints.size() > 2){
            // Compute total underground path length.
            double totalLen = 0;
            for(int i = 1; i < waypoints.size(); i++){
               Vec3 a = waypoints.get(i - 1), b = waypoints.get(i);
               totalLen += Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y) + (b.z - a.z) * (b.z - a.z)); // 3D dist so vertical shafts get time
            }
            double cumLen = 0;
            for(int i = 1; i < waypoints.size() - 1; i++){
               Vec3 a = waypoints.get(i - 1), b = waypoints.get(i);
               cumLen += Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y) + (b.z - a.z) * (b.z - a.z));
               double frac = totalLen > 0 ? cumLen / totalLen : (double) i / (waypoints.size() - 1);
               double t = sinkEnd + (travelEnd - sinkEnd) * frac;
               Vec3 wp = waypoints.get(i);
               builder.add(CameraKeyframe.at(t).pos(wp.x, wp.y - UNDERGROUND_DEPTH, wp.z).rot(yaw, pitch).interp(InterpolationType.LINEAR).build());
            }
         }
         
         builder.add(CameraKeyframe.at(travelEnd).pos(dest.x, dest.y - UNDERGROUND_DEPTH, dest.z).rot(yaw, pitch).interp(InterpolationType.LINEAR).build()).add(CameraKeyframe.at(1.0).pos(dest).rot(yaw, pitch).interp(InterpolationType.EASE_OUT).build());
         
         return builder.build();
      }
      
      @Override
      public void onStart(ServerPlayer player){
         super.onStart(player);
         player.setPose(Pose.STANDING);
         ServerLevel world = player.level();
         world.playSound(null, origin.x, origin.y, origin.z, SoundEvents.WARDEN_DIG, SoundSource.PLAYERS, 1.0f, 1.0f);
         spawnBreakParticles(world, origin);
      }
      
      @Override
      public void onTick(ServerPlayer player, int tick){
         super.onTick(player, tick);
         // Pose: swimming while underground, standing during sink/rise phases.
         if(tick >= SINK_TICKS && tick < riseStartTick){
            player.setPose(Pose.SWIMMING);
         }else{
            player.setPose(Pose.STANDING);
         }
         if(tick == riseStartTick){
            ServerLevel world = player.level();
            world.playSound(null, destination.x, destination.y, destination.z, SoundEvents.WARDEN_EMERGE, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnBreakParticles(world, destination);
         }
      }
      
      @Override
      public void onEnd(ServerPlayer player, boolean cancelled){
         super.onEnd(player, cancelled);
         player.setPose(Pose.STANDING);
      }
      
      private static void spawnBreakParticles(ServerLevel world, Vec3 feet){
         BlockPos below = BlockPos.containing(feet).below();
         BlockState state = world.getBlockState(below);
         if(!state.isAir()){
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), feet.x, feet.y + 0.1, feet.z, 30, 0.4, 0.15, 0.4, 0.18);
         }
         world.sendParticles(ParticleTypes.DUST_PLUME, feet.x, feet.y, feet.z, 100, 1, 0.25, 1, 0.25);
      }
   }
}
