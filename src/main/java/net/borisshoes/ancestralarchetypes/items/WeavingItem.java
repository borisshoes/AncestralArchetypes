package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.mixins.FallingBlockEntityAccessor;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class WeavingItem extends AbilityItem{
   
   public WeavingItem(Settings settings){
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
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      spawnCobwebBlock(player.getEntityWorld(),player);
      
      profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.WEAVING_WEB_COOLDOWN));
      player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(player.playerScreenHandler.syncId, player.playerScreenHandler.nextRevision(), player.getActiveHand() == Hand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getStackInHand(hand)));
      return ActionResult.SUCCESS;
   }
   
   private void spawnCobwebBlock(ServerWorld world, ServerPlayerEntity player){
      FallingBlockEntity fallingBlockEntity = FallingBlockEntityAccessor.newFallingBlock(world, player.getEyePos().getX(), player.getEyePos().getY(), player.getEyePos().getZ(), Blocks.COBWEB.getDefaultState());
      fallingBlockEntity.dropItem = false;
      world.spawnEntity(fallingBlockEntity);
      Vec3d lookingDir = player.getRotationVector();
      Vec3d vel = lookingDir.add(0,0.15,0).multiply(0.5);
      fallingBlockEntity.setVelocity(vel);
   }
}
