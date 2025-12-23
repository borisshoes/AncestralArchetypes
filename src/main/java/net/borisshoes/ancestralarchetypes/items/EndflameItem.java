package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.MathUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class EndflameItem extends AbilityItem{
   public EndflameItem(Properties settings){
      super(ArchetypeRegistry.ENDERFLAME, "\uD83D\uDD25", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.DRAGON_BREATH;
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
      
      if(player.isShiftKeyDown()){
         shootFireball(player);
         profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_FIREBALL_COOLDOWN));
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, user.getItemInHand(hand)));
      }else{
         player.startUsingItem(hand);
      }
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayer player)) return;
      
      final double range = 10.0;
      final double closeW = 1.5;
      final double farW = 5.5;
      double mul = 1.5*range;
      Vec3 boxStart = player.position().subtract(mul,mul,mul);
      Vec3 boxEnd = player.position().add(mul,mul,mul);
      AABB rangeBox = new AABB(boxStart,boxEnd);
      
      if(remainingUseTicks % 5 == 2){
         SoundUtils.playSound(world, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.6f, (float) (Math.random() * .5 + .5));
         
         List<Entity> entities = player.level().getEntities(player,rangeBox, e -> e instanceof LivingEntity);
         for(Entity e : entities){
            if(!(e instanceof LivingEntity entity)) continue;
            if(MathUtils.inCone(player.getEyePosition(),player.getLookAngle(),range,closeW,farW,e.getEyePosition())){
               entity.hurtServer(player.level(),player.damageSources().indirectMagic(player,player),(float) CONFIG.getDouble(ArchetypeRegistry.ENDERFLAME_BUFFET_DAMAGE));
            }
         }
      }
      
      double mod = 10;
      double percentage = 1.0 - (remainingUseTicks % mod / mod);
      double R = (farW-closeW)*percentage + closeW;
      DustColorTransitionOptions dust = new DustColorTransitionOptions(0xe400ff, 0xa106b8, 1.4f);
      double layerWidth = range / mod;
      
      Vec3 axis = player.getLookAngle();
      Vec3 upRef = Math.abs(axis.y) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
      Vec3 xBasis = axis.cross(upRef).normalize();
      Vec3 zBasis = axis.cross(xBasis).normalize();
      
      for(int i = 0; i < R*R*4; i++){
         double r = R * Math.sqrt(Math.random());
         double theta = Math.random() * 2 * Math.PI;
         Vec3 unRotatedOffset = new Vec3(r * Math.cos(theta), percentage*range, r * Math.sin(theta));
         Vec3 rotatedOffset = xBasis.scale(unRotatedOffset.x()).add(axis.scale(unRotatedOffset.y())).add(zBasis.scale(unRotatedOffset.z()));
         Vec3 pos = rotatedOffset.add(player.getEyePosition());
         player.level().sendParticles(dust,pos.x(),pos.y(),pos.z(),1,0.1,0.1+layerWidth/2.0,0.1,0.01);
      }
   }
   
   @Override
   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayer player)) return false;
      int cooldown = CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_BUFFET_COOLDOWN);
      profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double)remainingUseTicks/ getUseDuration(stack,user)))));
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return false;
   }
   
   @Override
   public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user){
      if(!(user instanceof ServerPlayer player)) return stack;
      profile(player).setAbilityCooldown(this.ability,CONFIG.getInt(ArchetypeRegistry.ENDERFLAME_BUFFET_COOLDOWN));
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, stack));
      return stack;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      return ItemUseAnimation.BOW;
   }
   
   @Override
   public int getUseDuration(ItemStack stack, LivingEntity user){
      return 200;
   }
   
   private void shootFireball(ServerPlayer player){
      double errorRad = Math.toRadians(0.25);
      
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
      
      DragonFireball smallFireballEntity = new DragonFireball(player.level(), player, rotatedVector.scale(1.5));
      smallFireballEntity.setPos(smallFireballEntity.getX(), player.getY(0.5) + 0.5, smallFireballEntity.getZ());
      player.level().addFreshEntity(smallFireballEntity);
      SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS,1f, 1.25f);
   }
}
