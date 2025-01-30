package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.WIND_CHARGE_VOLLEY;

public class WindChargeVolleyItem extends AbilityItem{
   
   public WindChargeVolleyItem(Settings settings){
      super(WIND_CHARGE_VOLLEY, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.BREEZE_ROD;
      }else{
         return Items.WIND_CHARGE;
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
      
      shootWindCharge(player,0.0f, 2.0f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      profile(player).setAbilityCooldown(this.ability, ArchetypeConfig.getInt(ArchetypeRegistry.WIND_CHARGE_COOLDOWN));
      SoundUtils.playSound(player.getServerWorld(),player.getBlockPos(),SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS,1f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().selectedSlot : 45, player.getStackInHand(hand)));
      return ActionResult.SUCCESS;
   }
   
   private void shootWindCharge(ServerPlayerEntity player, float firingErrorDeg, float power){
      double errorRad = Math.toRadians(firingErrorDeg);
      
      double randomAngle = player.getRandom().nextDouble() * 2 * Math.PI;
      double randomTheta = Math.acos(1 - player.getRandom().nextDouble() * (1 - Math.cos(errorRad)));
      
      Vec3d randomOrthogonal = player.getRotationVector().crossProduct(new Vec3d(
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5
      )).normalize();
      
      Vec3d rotatedVector = player.getRotationVector().multiply(Math.cos(randomTheta))
            .add(randomOrthogonal.multiply(Math.sin(randomTheta) * Math.cos(randomAngle)))
            .add(randomOrthogonal.crossProduct(player.getRotationVector()).multiply(Math.sin(randomTheta) * Math.sin(randomAngle)))
            .normalize();
      
      ServerWorld world = player.getServerWorld();
      WindChargeEntity windCharge = new WindChargeEntity(player, world, player.getPos().getX(), player.getEyePos().getY(), player.getPos().getZ());
      windCharge.setVelocity(rotatedVector.multiply(power));
      player.getWorld().spawnEntity(windCharge);
   }
}
