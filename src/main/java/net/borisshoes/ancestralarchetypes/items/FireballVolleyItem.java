package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.FIREBALL_VOLLEY;

public class FireballVolleyItem extends AbilityItem{
   
   public FireballVolleyItem(Properties settings){
      super(FIREBALL_VOLLEY, "\uD83D\uDD25", settings);
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
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      
      shootFireball(player,0.0f);
      player.startUsingItem(hand);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayer player)) return;
      if(remainingUseTicks % 5 == 2){
         shootFireball(player,5.0f);
      }
   }
   
   @Override
   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayer player)) return false;
      int cooldown = CONFIG.getInt(ArchetypeRegistry.FIREBALL_COOLDOWN);
      profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double)remainingUseTicks/ getUseDuration(stack,user)))));
      return false;
   }
   
   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user){
      if(!(user instanceof ServerPlayer player)) return stack;
      shootFireball(player,5.0f);
      shootFireball(player,5.0f);
      shootFireball(player,5.0f);
      profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.FIREBALL_COOLDOWN));
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      return ItemUseAnimation.BOW;
   }
   
   @Override
   public int getUseDuration(ItemStack stack, LivingEntity user){
      return 100;
   }
   
   private void shootFireball(ServerPlayer player, float firingErrorDeg){
      double errorRad = Math.toRadians(firingErrorDeg);
      
      double randomAngle = player.getRandom().nextDouble() * 2 * Math.PI;
      double randomTheta = Math.acos(1 - player.getRandom().nextDouble() * (1 - Math.cos(errorRad)));
      
      Vec3 randomOrthogonal = player.getLookAngle().cross(new Vec3(
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5,
            player.getRandom().nextDouble() - 0.5
      )).normalize();
      
      Vec3 rotatedVector = player.getLookAngle().scale(Math.cos(randomTheta))
            .add(randomOrthogonal.scale(Math.sin(randomTheta) * Math.cos(randomAngle)))
            .add(randomOrthogonal.cross(player.getLookAngle()).scale(Math.sin(randomTheta) * Math.sin(randomAngle)))
            .normalize();
      
      SmallFireball smallFireballEntity = new SmallFireball(player.level(), player, rotatedVector.scale(1.5));
      smallFireballEntity.setPos(smallFireballEntity.getX(), player.getY(0.5) + 0.5, smallFireballEntity.getZ());
      player.level().addFreshEntity(smallFireballEntity);
      SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,1f, 0.75f + player.getRandom().nextFloat()*0.5f);
   }
}
