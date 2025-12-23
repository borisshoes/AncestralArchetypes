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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.WIND_CHARGE_VOLLEY;

public class WindChargeVolleyItem extends AbilityItem{
   
   public WindChargeVolleyItem(Properties settings){
      super(WIND_CHARGE_VOLLEY,"\uD83D\uDCA8", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.BREEZE_ROD;
      }else{
         return Items.WIND_CHARGE;
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
      
      shootWindCharge(player,0.0f, 2.0f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      shootWindCharge(player,7.0f,0.75f);
      profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.WIND_CHARGE_COOLDOWN));
      SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS,1f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
      return InteractionResult.SUCCESS;
   }
   
   private void shootWindCharge(ServerPlayer player, float firingErrorDeg, float power){
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
      
      ServerLevel world = player.level();
      WindCharge windCharge = new WindCharge(player, world, player.position().x(), player.getEyePosition().y(), player.position().z());
      windCharge.setDeltaMovement(rotatedVector.scale(power));
      player.level().addFreshEntity(windCharge);
   }
}
