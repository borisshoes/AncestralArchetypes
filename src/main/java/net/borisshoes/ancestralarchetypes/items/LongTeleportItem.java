package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class LongTeleportItem extends AbilityItem{
   public LongTeleportItem(Settings settings){
      super(ArchetypeRegistry.LONG_TELEPORT, "\uD83D\uDC41", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.CLAY_BALL;
      }else{
         return Items.ENDER_PEARL;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) <= 0 && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)){
         Vec3d spot = findTeleportSpot(world,player);
         if(spot != null){
            ParticleEffectUtils.circle(world,player,spot.subtract(0,0,0), ParticleTypes.ENCHANTED_HIT,0.5,12,1,0.1,0);
            world.spawnParticles(player, ParticleTypes.WITCH, true,true, spot.x,spot.y,spot.z,5,.15,.15,.15,0);
         }
      }
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      if(teleport(player.getEntityWorld(),player)){
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.RANDOM_TELEPORT_COOLDOWN));
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.SUCCESS;
      }else{
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.FAIL;
      }
   }
   
   private boolean teleport(ServerWorld world, ServerPlayerEntity user){
      Vec3d spot = findTeleportSpot(world,user);
      if(spot == null) return false;
      if(user.teleport(spot.x,spot.y,spot.z,true)){
         world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS);
         return true;
      }
      return false;
   }
   
   private Vec3d findTeleportSpot(ServerWorld world, ServerPlayerEntity user){
      Vec3d direction = user.getRotationVector().normalize();
      double maxRange = CONFIG.getDouble(ArchetypeRegistry.LONG_TELEPORT_DISTANCE);
      double leniencyRange = 1.5;
      Vec3d origin = user.getEntityPos();
      double distStep = 0.5;
      double radialStep = 0.5;
      double dropStep = 0.25;
      double maxDistSq = (maxRange + leniencyRange) * (maxRange + leniencyRange);
      Vec3d upRef = Math.abs(direction.y) < 0.999 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
      Vec3d right = direction.crossProduct(upRef).normalize();
      Vec3d up = direction.crossProduct(right).normalize();
      for(double d = maxRange; d >= 0.0; d -= distStep){
         Vec3d center = origin.add(direction.multiply(d));
         for(double r = 0.0; r <= leniencyRange + 1e-9; r += radialStep){
            int slices = r == 0.0 ? 1 : 12;
            for(int k = 0; k < slices; k++){
               double a = slices == 1 ? 0.0 : (2.0 * Math.PI * k) / slices;
               Vec3d lateral = right.multiply(r * Math.cos(a)).add(up.multiply(r * Math.sin(a)));
               Vec3d base = center.add(lateral);
               double[] yNudges = new double[]{0.0, 0.5, -0.5, 1.0, -1.0};
               for(double yOff : yNudges){
                  Vec3d candidate = new Vec3d(base.x, base.y + yOff, base.z);
                  if(!isSpaceClearFor(user, world, candidate)) continue;
                  if(hasGroundSupport(world, user, candidate)){
                     return candidate;
                  }
                  Vec3d down = candidate;
                  while(origin.squaredDistanceTo(down) <= maxDistSq && down.y > world.getBottomY()){
                     down = down.add(0.0, -dropStep, 0.0);
                     if(!isSpaceClearFor(user, world, down)) break;
                     if(hasGroundSupport(world, user, down)){
                        return down;
                     }
                  }
               }
            }
         }
      }
      return null;
   }
   
   private boolean hasGroundSupport(World world, Entity entity, Vec3d targetPos){
      Vec3d delta = targetPos.subtract(entity.getEntityPos());
      Box targetBox = entity.getBoundingBox().offset(delta);
      double eps = 1.0 / 16.0;
      Box floorProbe = new Box(targetBox.minX, targetBox.minY - eps, targetBox.minZ, targetBox.maxX, targetBox.minY, targetBox.maxZ);
      return world.getBlockCollisions(entity, floorProbe).iterator().hasNext();
   }
   
   private boolean isSpaceClearFor(Entity entity, World world, Vec3d targetPos) {
      Vec3d delta = targetPos.subtract(entity.getEntityPos());
      Box targetBox = entity.getBoundingBox().offset(delta);
      return world.isSpaceEmpty(entity, targetBox, true);
   }
   
}
