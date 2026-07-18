package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.entities.CreakingHeartEntity;
import net.borisshoes.borislib.utils.SoundUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class CreakingHeartItem extends AbilityItem {
   
   public static final String SHOW_ACTIVE = "active";
   
   public CreakingHeartItem(Properties settings){
      super(ArchetypeRegistry.CREAKING_HEART, "❤", settings);
   }
   
   @Override
   public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context, HolderLookup.Provider lookup){
      ItemStack baseStack = super.getPolymerItemStack(itemStack, tooltipType, context, lookup);
      boolean active = archetypes$ITEM_DATA.getBooleanProperty(itemStack, SHOW_ACTIVE);
      List<String> stringList = new ArrayList<>();
      if(active){
         stringList.add("on");
      }else{
         stringList.add("off");
      }
      baseStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(new ArrayList<>(), new ArrayList<>(), stringList, new ArrayList<>()));
      return baseStack;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.CLAY_BALL;
      }else{
         return Items.CREAKING_HEART;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         CreakingHeartEntity heart = profile.getCreakingHeart();
         boolean isAlive = heart != null && heart.isAlive();
         if(isAlive){
            int maxHp = (int) heart.getMaxHealth() + 1;
            if(stack.getMaxDamage() != maxHp)
               stack.set(DataComponents.MAX_DAMAGE, maxHp);
            stack.setDamageValue(maxHp - (int) heart.getHealth());
         }else{
            stack.remove(DataComponents.MAX_DAMAGE);
            stack.remove(DataComponents.DAMAGE);
         }
         archetypes$ITEM_DATA.putProperty(stack, SHOW_ACTIVE, isAlive);
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.25f, 0.8f);
         return InteractionResult.PASS;
      }
      
      if(profile.hasCreakingHeart()){
         profile.getCreakingHeart().discard();
         world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.CREAKING_DEACTIVATE, SoundSource.NEUTRAL, 2F, 0.3F + world.getRandom().nextFloat() * 0.3F);
      }else{
         CreakingHeartEntity newHeart = ArchetypeRegistry.CREAKING_HEART_ENTITY.create(world, new EntitySpawnRequest(EntitySpawnReason.SPAWN_ITEM_USE, true));
         if(newHeart != null){
            newHeart.setOwner(player);
            newHeart.snapTo(player.position().add(0, player.getBbHeight() / 2, 0));
            player.level().tryAddFreshEntityWithPassengers(newHeart);
            profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_COOLDOWN));
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.NEUTRAL, 2F, 0.3F + world.getRandom().nextFloat() * 0.3F);
         }
      }
      
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
      return InteractionResult.SUCCESS;
   }
}
