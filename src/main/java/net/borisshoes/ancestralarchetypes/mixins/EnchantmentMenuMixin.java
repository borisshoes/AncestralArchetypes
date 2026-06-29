package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.misc.MetamorphTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(EnchantmentMenu.class)
public class EnchantmentMenuMixin {
   
   @ModifyArg(method = "lambda$slotsChanged$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"), index = 2)
   private int archetypes$bookshelfMetamorph(int bookcases, @Local(argsOnly = true, name = "level") Level level, @Local(argsOnly = true, name = "pos") BlockPos pos){
      AtomicInteger cases = new AtomicInteger(bookcases);
      List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(4));
      players.removeIf(player -> AncestralArchetypes.profile(player).getMetamorph() != MetamorphTypes.BOOKSHELF);
      players.forEach(player -> {
         float dist = Mth.sqrt((float) player.distanceToSqr(pos.getBottomCenter()));
         cases.addAndGet(Math.max(0,(int) (5 * (4 - dist))));
      });
      return Math.max(bookcases,Math.min(20,cases.get())); // This CANNOT be replaced by clamp, the IDE is lying
   }
}
