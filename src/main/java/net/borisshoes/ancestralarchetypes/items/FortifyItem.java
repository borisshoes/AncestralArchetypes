package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class FortifyItem extends AbilityItem{
   public FortifyItem(Properties settings){
      super(ArchetypeRegistry.FORTIFY, "⌛", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.DIAMOND;
      }else{
         return Items.DIAMOND_CHESTPLATE;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         return;
      }
      double fortifyPercentage = ((double)profile.getFortifyTime() / profile.getMaxFortifyTime());
      if(fortifyPercentage >= 1.0) return;
      int fortifyValue = (int) (fortifyPercentage * 100);
      char[] unicodeChars = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
      StringBuilder message = new StringBuilder("\uD83D\uDEE1 ");
      for (int i = 0; i < 10; i++) {
         int segmentValue = fortifyValue - (i * 10);
         if (segmentValue <= 0) {
            message.append(unicodeChars[0]);
         } else if (segmentValue >= 10) {
            message.append(unicodeChars[unicodeChars.length - 1]);
         } else {
            int charIndex = (int) ((double) segmentValue / 10 * (unicodeChars.length - 1));
            message.append(unicodeChars[charIndex]);
         }
      }
      message.append(" \uD83D\uDEE1");
      player.displayClientMessage(Component.literal(message.toString()).withColor(profile.getSubArchetype().getColor()), true);
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
      SoundUtils.playSound(world,player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS,1f,1.5f);
      profile.setFortifyActive(true);
      player.startUsingItem(hand);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      float remainingTime = profile.getFortifyTime();
      
      double fortifyPercentage = ((double)profile.getFortifyTime() / profile.getMaxFortifyTime());
      int fortifyValue = (int) (fortifyPercentage * 100);
      char[] unicodeChars = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
      StringBuilder message = new StringBuilder("\uD83D\uDEE1 ");
      for (int i = 0; i < 10; i++) {
         int segmentValue = fortifyValue - (i * 10);
         if (segmentValue <= 0) {
            message.append(unicodeChars[0]);
         } else if (segmentValue >= 10) {
            message.append(unicodeChars[unicodeChars.length - 1]);
         } else {
            int charIndex = (int) ((double) segmentValue / 10 * (unicodeChars.length - 1));
            message.append(unicodeChars[charIndex]);
         }
      }
      message.append(" \uD83D\uDEE1");
      player.displayClientMessage(Component.literal(message.toString()).withColor(profile.getSubArchetype().getColor()), true);
      
      if(remainingTime < 1 || profile.getAbilityCooldown(this.ability) > 0){
         player.releaseUsingItem();
         profile.setFortifyActive(false);
         SoundUtils.playSound(world,player.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS,1f,0.75f);
      }
   }
   
   @Override
   public boolean releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayer player)) return false;
      PlayerArchetypeData profile = profile(player);
      profile.setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.FORTIFY_COOLDOWN));
      profile.setFortifyActive(false);
      return false;
   }
   
   @Override
   public ItemUseAnimation getUseAnimation(ItemStack stack){
      return ItemUseAnimation.BLOCK;
   }
   
   @Override
   public int getUseDuration(ItemStack stack, LivingEntity user){
      return 72000;
   }
}
