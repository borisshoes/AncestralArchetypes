package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.mixins.FallingBlockEntityAccessor;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class WeavingItem extends AbilityItem{
   
   public WeavingItem(Properties settings){
      super(ArchetypeRegistry.WEAVING, "\uD83D\uDD78", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.SPIDER_EYE;
      }else{
         return Items.COBWEB;
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
      
      spawnCobwebBlock(player.level(),player);
      
      profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.WEAVING_WEB_COOLDOWN));
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
      return InteractionResult.SUCCESS;
   }
   
   private void spawnCobwebBlock(ServerLevel world, ServerPlayer player){
      FallingBlockEntity fallingBlockEntity = FallingBlockEntityAccessor.newFallingBlock(world, player.getEyePosition().x(), player.getEyePosition().y(), player.getEyePosition().z(), Blocks.COBWEB.defaultBlockState());
      fallingBlockEntity.dropItem = false;
      world.addFreshEntity(fallingBlockEntity);
      Vec3 lookingDir = player.getLookAngle();
      Vec3 vel = lookingDir.add(0,0.15,0).scale(0.5);
      fallingBlockEntity.setDeltaMovement(vel);
   }
}
