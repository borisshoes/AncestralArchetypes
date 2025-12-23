package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeParticles;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.GUARDIAN_RAY;

public class GuardianRayItem extends AbilityItem{
   public GuardianRayItem(Properties settings){
      super(GUARDIAN_RAY, "⇏", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.PRISMARINE_CRYSTALS;
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
      player.startUsingItem(hand);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayer player)) return;
      int useTime = this.getUseDuration(stack, user) - remainingUseTicks;
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      int duration = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_DURATION);
      
      MinecraftUtils.LasercastResult lasercast = MinecraftUtils.lasercast(world, player.getEyePosition(), player.getForward(), 25, false, player);
      ArchetypeParticles.guardianRay(player.level(),lasercast.startPos().subtract(0,player.getBbHeight()/3,0),lasercast.endPos(), useTime);
      
      if(useTime < windup){ // Windup
         if(useTime % 5 == 0) SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS,0.3f, 0.5f + 1.2f*((float) useTime / windup));
      }else if(useTime < (windup+duration)){ // Shoot
         if(useTime == windup) {
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.GUARDIAN_AMBIENT_LAND, SoundSource.PLAYERS,1.2f, 0.8f);
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,1.2f, 1.2f);
         }
         
         float damage = (float) CONFIG.getDouble(ArchetypeRegistry.GUARDIAN_RAY_DAMAGE);
         if(useTime % 15 == 0){
            for(Entity hit : lasercast.sortedHits()){
               hit.hurtServer(player.level(), player.damageSources().indirectMagic(player,player), damage);
            }
         }
         
         if(useTime % 20 == 0){
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.2f, 1.2f);
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.GUARDIAN_AMBIENT_LAND, SoundSource.PLAYERS,0.75f, 0.7f);
         }
      }else{ // Reset
         player.releaseUsingItem();
      }
   }
   
   @Override
   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayer player)) return false;
      int useTime = this.getUseDuration(stack, user) - remainingUseTicks;
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      int duration = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_DURATION);
      int cooldown = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_COOLDOWN);
      
      if(useTime > windup){
         profile(player).setAbilityCooldown(this.ability, (int) Math.max(0.25*cooldown,cooldown*(1 - ((double) (useTime - windup) / duration))));
      }
      return false;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      return ItemUseAnimation.BOW;
   }
   
   @Override
   public int getUseDuration(ItemStack stack, LivingEntity user){
      return 72000;
   }
}
