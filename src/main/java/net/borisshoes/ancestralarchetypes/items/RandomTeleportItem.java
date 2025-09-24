package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.TeleportRandomlyConsumeEffect;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class RandomTeleportItem extends AbilityItem{
   public RandomTeleportItem(Settings settings){
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
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      if(teleport(player.getWorld(),player)){
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.RANDOM_TELEPORT_COOLDOWN));
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.SUCCESS;
      }else{
         player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
         return ActionResult.FAIL;
      }
   }
   
   private boolean teleport(ServerWorld world, ServerPlayerEntity user){
      double diameter = CONFIG.getDouble(ArchetypeRegistry.RANDOM_TELEPORT_RANGE)*2.0;
      boolean bl = false;
      
      for (int i = 0; i < 16; i++) {
         double d = user.getX() + (user.getRandom().nextDouble() - 0.5) * diameter;
         double e = MathHelper.clamp(
               user.getY() + (user.getRandom().nextDouble() - 0.5) * diameter,
               world.getBottomY(),
               world.getBottomY() + world.getLogicalHeight() - 1
         );
         double f = user.getZ() + (user.getRandom().nextDouble() - 0.5) * diameter;
         if (user.hasVehicle()) {
            user.stopRiding();
         }
         
         Vec3d vec3d = user.getPos();
         if (user.teleport(d, e, f, true)) {
            world.emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
            SoundCategory soundCategory;
            SoundEvent soundEvent;
            soundEvent = SoundEvents.ENTITY_SHULKER_TELEPORT;
            soundCategory = SoundCategory.PLAYERS;
            
            world.playSound(null, user.getX(), user.getY(), user.getZ(), soundEvent, soundCategory);
            user.onLanding();
            bl = true;
            break;
         }
      }
      
      if (bl) {
         user.clearCurrentExplosion();
      }
      
      return bl;
   }
}
