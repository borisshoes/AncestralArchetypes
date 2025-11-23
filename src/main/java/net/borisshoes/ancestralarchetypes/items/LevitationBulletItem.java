package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.callbacks.DeglowTimerCallback;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.entities.LevitationBulletEntity;
import net.borisshoes.ancestralarchetypes.events.BulletTargetEvent;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.events.Event;
import net.borisshoes.borislib.utils.MathUtils;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class LevitationBulletItem extends AbilityItem {
   public LevitationBulletItem(Settings settings){
      super(ArchetypeRegistry.LEVITATION_BULLET, "❋", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.GHAST_TEAR;
      }else{
         return Items.END_CRYSTAL;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) <= 0 && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)){
         LivingEntity target = getTarget(world,player);
         if(target != null && player.getEntityWorld().getServer().getTicks() % 4 == 0){
            boolean shouldGlow = Event.getEventsOfType(BulletTargetEvent.class).stream().noneMatch(e -> e.player.getId() == player.getId() && e.target.getId() == target.getId());
            if(shouldGlow){
               addGlow(player,target,Formatting.LIGHT_PURPLE);
               BorisLib.addTickTimerCallback(new DeglowTimerCallback(player,target));
            }
            Event.addEvent(new BulletTargetEvent(player,target));
         }
      }
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED, Formatting.ITALIC), true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH, 0.25f, 0.8f);
         return ActionResult.PASS;
      }
      
      LivingEntity target = getTarget(player.getEntityWorld(),player);
      if(target != null){
         for(int i = 0; i < CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_COUNT); i++){
            LevitationBulletEntity bullet = new LevitationBulletEntity(player.getEntityWorld(), player, target, Direction.Axis.pickRandomAxis(player.getRandom()));
            bullet.setPosition(MathUtils.randomSpherePoint(player.getEyePos().add(0,0.5,0),1));
            player.getEntityWorld().spawnEntity(bullet);
         }
         SoundUtils.playSound(player.getEntityWorld(),player.getBlockPos(),SoundEvents.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 2.0F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_COOLDOWN));
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.SUCCESS;
      }else{
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.FAIL;
      }
   }
   
   private LivingEntity getTarget(ServerWorld world, ServerPlayerEntity user){
      Vec3d eye = user.getEyePos();
      Vec3d look = user.getRotationVector().normalize();
      int viewChunks = world.getServer().getPlayerManager().getViewDistance();
      double maxRange = viewChunks * 16.0;
      Optional<LivingEntity> entity = MinecraftUtils.lasercast(world,eye,look,maxRange,false,user)
            .sortedHits().stream().filter(e -> e instanceof LivingEntity && eye.squaredDistanceTo(e.getEntityPos()) <= maxRange * maxRange)
            .map(e -> (LivingEntity)e).findFirst();
      if(entity.isPresent()){
         return entity.get();
      }
      double maxAngleDeg = 5.0;
      double bestScore = Double.POSITIVE_INFINITY;
      LivingEntity best = null;
      Box search = new Box(eye, eye).expand(maxRange);
      for(LivingEntity e : world.getEntitiesByClass(LivingEntity.class, search, le -> le.isAlive() && le != user && !le.isSpectator())){
         Vec3d targetEye = e.getEyePos();
         Vec3d to = targetEye.subtract(eye);
         double dist = to.length();
         if(dist <= 1.0e-6 || dist > maxRange) continue;
         Vec3d dir = to.multiply(1.0 / dist);
         double dot = look.dotProduct(dir);
         dot = Math.max(-1.0, Math.min(1.0, dot));
         double angleDeg = Math.toDegrees(Math.acos(dot));
         if(angleDeg > maxAngleDeg) continue;
         if(!hasLineOfSight(world, user, eye, targetEye)) continue;
         double score = angleDeg + (dist / 25.0);
         if(score < bestScore){
            bestScore = score;
            best = e;
         }
      }
      return best;
   }
   
   
   private boolean hasLineOfSight(ServerWorld world, Entity viewer, Vec3d from, Vec3d to){
      BlockHitResult hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, viewer));
      return hit.getType() == HitResult.Type.MISS;
   }
   
   private static void addGlow(ServerPlayerEntity viewer, LivingEntity target, Formatting color){
      String teamName = "glow_" + viewer.getUuidAsString() + "_" + target.getId();
      Team team = new Team(new Scoreboard(), teamName);
      team.setColor(color);
      team.setFriendlyFireAllowed(true);
      team.setShowFriendlyInvisibles(false);
      team.getPlayerList().add(target.getNameForScoreboard());
      byte flags = target.getDataTracker().get(EntityAccessor.getFLAGS());
      byte glowing = (byte)(flags | (1 << 6));
      List<DataTracker.SerializedEntry<?>> entries = List.of(DataTracker.SerializedEntry.of(EntityAccessor.getFLAGS(), glowing));
      viewer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(target.getId(), entries));
      viewer.networkHandler.sendPacket(TeamS2CPacket.updateTeam(team,true));
   }
   
   public static void removeGlow(ServerPlayerEntity viewer, LivingEntity target){
      String teamName = "glow_" + viewer.getUuidAsString() + "_" + target.getId();
      Team team = new Team(new Scoreboard(), teamName);
      byte flags = target.getDataTracker().get(EntityAccessor.getFLAGS());
      viewer.networkHandler.sendPacket(TeamS2CPacket.updateRemovedTeam(team));
      viewer.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(target.getId(), List.of(DataTracker.SerializedEntry.of(EntityAccessor.getFLAGS(), flags))));
   }
}
