package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.arcananovum.core.ArcanaItemContainer;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.arcananovum.utils.ArcanaItemUtils.arcanaInvHelper;

@Pseudo
@Mixin(ArcanaItemUtils.class)
public class ArcanaItemUtilsMixin {

   @Inject(method = "getArcanaInventory", at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z"))
   private static void archetypes$addArchetypeArcanaInventories(ServerPlayer player, CallbackInfoReturnable<List<ArcanaItemUtils.ArcanaInvItem>> cir, @Local(name = "arcanaInv") List<ArcanaItemUtils.ArcanaInvItem> arcanaInv){
      PlayerArchetypeData data = AncestralArchetypes.profile(player);
      ArcanaItemContainer shulkerContainer = new ArcanaItemContainer(
            Identifier.fromNamespaceAndPath(MOD_ID,"shulker_backpack"),
            data.getBackpackInventory(), data.getBackpackInventory().getContainerSize(),40,
            Component.literal("BP"),
            ArchetypeRegistry.BACKPACK.getName(),
            0.5);
      ArcanaItemContainer mountContainer = new ArcanaItemContainer(
            Identifier.fromNamespaceAndPath(MOD_ID,"mount_inventory"),
            data.getMountInventory(), data.getMountInventory().getContainerSize(),60,
            Component.literal("MI"),
            Component.translatable("text.ancestralarchetypes.spirit_mount_inventory"),
            0.5);
      arcanaInvHelper(data.getBackpackInventory(),arcanaInv,new ArrayList<>(List.of(shulkerContainer)));
      arcanaInvHelper(data.getMountInventory(),arcanaInv,new ArrayList<>(List.of(mountContainer)));
   }
}
