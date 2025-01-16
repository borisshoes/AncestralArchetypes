package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class ChangeItem extends Item implements PolymerItem {
   
   public ChangeItem(Settings settings){
      super(settings.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"change_item"))));
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      boolean couldChange = profile.canChangeArchetype();
      profile.increaseAllowedChanges(ArchetypeConfig.getInt(ArchetypeRegistry.CHANGES_PER_CHANGE_ITEM));
      if(!couldChange && profile.canChangeArchetype()){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.change_reminder").styled(s ->
               s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/archetypes changeArchetype"))
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("text.ancestralarchetypes.change_hover")))
                     .withColor(Formatting.AQUA)));
      }
      player.getItemCooldownManager().set(user.getStackInHand(hand),20);
      user.getStackInHand(hand).decrementUnlessCreative(1,player);
      SoundUtils.playSongToPlayer(player,SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,0.3f,1);
      return ActionResult.SUCCESS;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.NETHER_STAR;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.of(MOD_ID,"change_item");
   }
}
