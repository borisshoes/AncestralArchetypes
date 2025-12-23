package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

public class ChangeItem extends Item implements PolymerItem {
   
   public ChangeItem(Properties settings){
      super(settings.setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID,"change_item"))));
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      boolean couldChange = profile.canChangeArchetype();
      profile.increaseAllowedChanges(CONFIG.getInt(ArchetypeRegistry.CHANGES_PER_CHANGE_ITEM));
      if(!couldChange && profile.canChangeArchetype()){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.change_reminder").withStyle(s ->
               s.withClickEvent(new ClickEvent.RunCommand("/archetypes changeArchetype"))
                     .withHoverEvent(new HoverEvent.ShowText(Component.translatable("text.ancestralarchetypes.change_hover")))
                     .withColor(ChatFormatting.AQUA)));
      }
      player.getCooldowns().addCooldown(user.getItemInHand(hand),20);
      user.getItemInHand(hand).consume(1,player);
      SoundUtils.playSongToPlayer(player, SoundEvents.RESPAWN_ANCHOR_CHARGE,0.3f,1);
      return InteractionResult.SUCCESS;
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.NETHER_STAR;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      return Identifier.fromNamespaceAndPath(MOD_ID,"change_item");
   }
}
