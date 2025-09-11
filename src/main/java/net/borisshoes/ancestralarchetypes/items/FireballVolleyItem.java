package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.FIREBALL_VOLLEY;

public class FireballVolleyItem extends AbilityItem{
   
   public FireballVolleyItem(Settings settings){
      super(FIREBALL_VOLLEY, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.BLAZE_ROD;
      }else{
         return Items.FIRE_CHARGE;
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
      
      shootFireball(player,0.0f);
      player.setCurrentHand(hand);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayerEntity player)) return;
      if(remainingUseTicks % 5 == 2){
         shootFireball(player,5.0f);
      }
   }
   
   @Override
   public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      int cooldown = CONFIG.getInt(ArchetypeRegistry.FIREBALL_COOLDOWN);
      profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double)remainingUseTicks/getMaxUseTime(stack,user)))));
      return false;
   }
   
   @Override
   public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user){
      if(!(user instanceof ServerPlayerEntity player)) return stack;
      shootFireball(player,5.0f);
      shootFireball(player,5.0f);
      shootFireball(player,5.0f);
      profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.FIREBALL_COOLDOWN));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack){
      return UseAction.BOW;
   }
   
   @Override
   public int getMaxUseTime(ItemStack stack, LivingEntity user){
      return 100;
   }
   
   private void shootFireball(ServerPlayerEntity player, float firingErrorDeg){
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
      
      SmallFireballEntity smallFireballEntity = new SmallFireballEntity(player.getWorld(), player, rotatedVector.multiply(1.5));
      smallFireballEntity.setPosition(smallFireballEntity.getX(), player.getBodyY(0.5) + 0.5, smallFireballEntity.getZ());
      player.getWorld().spawnEntity(smallFireballEntity);
      SoundUtils.playSound(player.getWorld(),player.getBlockPos(),SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS,1f, 0.75f + player.getRandom().nextFloat()*0.5f);
   }
}
