package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.callbacks.DeglowTimerCallback;
import net.borisshoes.ancestralarchetypes.entities.LevitationBulletEntity;
import net.borisshoes.ancestralarchetypes.events.BulletTargetEvent;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.events.Event;
import net.borisshoes.borislib.utils.MathUtils;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class LevitationBulletItem extends AbilityItem {
   public LevitationBulletItem(Properties settings){
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
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) <= 0 && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)){
         LivingEntity target = getTarget(world,player);
         if(target != null && player.level().getServer().getTickCount() % 4 == 0){
            boolean shouldGlow = Event.getEventsOfType(BulletTargetEvent.class).stream().noneMatch(e -> e.player.getId() == player.getId() && e.target.getId() == target.getId());
            if(shouldGlow){
               addGlow(player,target, ChatFormatting.LIGHT_PURPLE);
               BorisLib.addTickTimerCallback(new DeglowTimerCallback(player,target));
            }
            Event.addEvent(new BulletTargetEvent(player,target));
         }
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.25f, 0.8f);
         return InteractionResult.PASS;
      }
      
      LivingEntity target = getTarget(player.level(),player);
      if(target != null){
         for(int i = 0; i < CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_COUNT); i++){
            LevitationBulletEntity bullet = new LevitationBulletEntity(player.level(), player, target, Direction.Axis.getRandom(player.getRandom()));
            bullet.setPos(MathUtils.randomSpherePoint(player.getEyePosition().add(0,0.5,0),1));
            player.level().addFreshEntity(bullet);
         }
         SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 2.0F, (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.LEVITATION_BULLET_COOLDOWN));
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.SUCCESS;
      }else{
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
   }
   
   private LivingEntity getTarget(ServerLevel world, ServerPlayer user){
      Vec3 eye = user.getEyePosition();
      Vec3 look = user.getLookAngle().normalize();
      int viewChunks = world.getServer().getPlayerList().getViewDistance();
      double maxRange = viewChunks * 16.0;
      Optional<LivingEntity> entity = MinecraftUtils.lasercast(world,eye,look,maxRange,false,user)
            .sortedHits().stream().filter(e -> e instanceof LivingEntity && eye.distanceToSqr(e.position()) <= maxRange * maxRange)
            .map(e -> (LivingEntity)e).findFirst();
      if(entity.isPresent()){
         return entity.get();
      }
      double maxAngleDeg = 5.0;
      double bestScore = Double.POSITIVE_INFINITY;
      LivingEntity best = null;
      AABB search = new AABB(eye, eye).inflate(maxRange);
      for(LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, search, le -> le.isAlive() && le != user && !le.isSpectator())){
         Vec3 targetEye = e.getEyePosition();
         Vec3 to = targetEye.subtract(eye);
         double dist = to.length();
         if(dist <= 1.0e-6 || dist > maxRange) continue;
         Vec3 dir = to.scale(1.0 / dist);
         double dot = look.dot(dir);
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
   
   
   private boolean hasLineOfSight(ServerLevel world, Entity viewer, Vec3 from, Vec3 to){
      BlockHitResult hit = world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
      return hit.getType() == HitResult.Type.MISS;
   }
   
   private static void addGlow(ServerPlayer viewer, LivingEntity target, ChatFormatting color){
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      team.setColor(color);
      team.setAllowFriendlyFire(true);
      team.setSeeFriendlyInvisibles(false);
      team.getPlayers().add(target.getScoreboardName());
      byte flags = target.getEntityData().get(EntityAccessor.getFLAGS());
      byte glowing = (byte)(flags | (1 << 6));
      List<SynchedEntityData.DataValue<?>> entries = List.of(SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), glowing));
      viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entries));
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team,true));
   }
   
   public static void removeGlow(ServerPlayer viewer, LivingEntity target){
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      byte flags = target.getEntityData().get(EntityAccessor.getFLAGS());
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
      viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), List.of(SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), flags))));
   }
}
