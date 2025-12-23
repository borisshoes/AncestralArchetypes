package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class RandomTeleportItem extends AbilityItem{
   public RandomTeleportItem(Properties settings){
      super(ArchetypeRegistry.RANDOM_TELEPORT, "۞", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.POPPED_CHORUS_FRUIT;
      }else{
         return Items.CHORUS_FRUIT;
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
      
      if(teleport(player.level(),player)){
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.RANDOM_TELEPORT_COOLDOWN));
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.SUCCESS;
      }else{
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
   }
   
   private boolean teleport(ServerLevel world, ServerPlayer user){
      double diameter = CONFIG.getDouble(ArchetypeRegistry.RANDOM_TELEPORT_RANGE)*2.0;
      boolean bl = false;
      
      for (int i = 0; i < 16; i++) {
         double d = user.getX() + (user.getRandom().nextDouble() - 0.5) * diameter;
         double e = Mth.clamp(
               user.getY() + (user.getRandom().nextDouble() - 0.5) * diameter,
               world.getMinY(),
               world.getMinY() + world.getLogicalHeight() - 1
         );
         double f = user.getZ() + (user.getRandom().nextDouble() - 0.5) * diameter;
         if (user.isPassenger()) {
            user.stopRiding();
         }
         
         Vec3 vec3d = user.position();
         if (user.randomTeleport(d, e, f, true)) {
            world.gameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Context.of(user));
            SoundSource soundCategory;
            SoundEvent soundEvent;
            soundEvent = SoundEvents.SHULKER_TELEPORT;
            soundCategory = SoundSource.PLAYERS;
            
            world.playSound(null, user.getX(), user.getY(), user.getZ(), soundEvent, soundCategory);
            user.resetFallDistance();
            bl = true;
            break;
         }
      }
      
      if (bl) {
         user.resetCurrentImpulseContext();
      }
      
      return bl;
   }
}
