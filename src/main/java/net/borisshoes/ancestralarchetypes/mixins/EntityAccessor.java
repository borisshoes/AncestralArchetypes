package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
   @Invoker
   void callSetWorld(World var1);
   
   @Invoker("isBeingRainedOn")
   boolean rainedOn();
   
   @Accessor("FLAGS")
   static TrackedData<Byte> getFLAGS(){
      throw new AssertionError();
   }
}
