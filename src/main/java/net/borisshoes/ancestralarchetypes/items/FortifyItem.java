package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class FortifyItem extends AbilityItem{
   public FortifyItem(Settings settings){
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
   public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
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
      player.sendMessage(Text.literal(message.toString()).withColor(profile.getSubArchetype().getColor()), true);
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
      SoundUtils.playSound(world,player.getBlockPos(),SoundEvents.ENTITY_IRON_GOLEM_REPAIR,SoundCategory.PLAYERS,1f,1.5f);
      profile.setFortifyActive(true);
      player.setCurrentHand(hand);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks){
      if(!(user instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
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
      player.sendMessage(Text.literal(message.toString()).withColor(profile.getSubArchetype().getColor()), true);
      
      if(remainingTime < 1 || profile.getAbilityCooldown(this.ability) > 0){
         player.stopUsingItem();
         profile.setFortifyActive(false);
         SoundUtils.playSound(world,player.getBlockPos(),SoundEvents.ENTITY_IRON_GOLEM_DAMAGE,SoundCategory.PLAYERS,1f,0.75f);
      }
   }
   
   @Override
   public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
      if(!(user instanceof ServerPlayerEntity player)) return false;
      IArchetypeProfile profile = profile(player);
      profile.setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.FORTIFY_COOLDOWN));
      profile.setFortifyActive(false);
      return false;
   }
   
   @Override
   public UseAction getUseAction(ItemStack stack){
      return UseAction.BLOCK;
   }
   
   @Override
   public int getMaxUseTime(ItemStack stack, LivingEntity user){
      return 72000;
   }
}
