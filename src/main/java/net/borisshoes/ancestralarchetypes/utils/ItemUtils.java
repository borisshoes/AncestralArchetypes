package net.borisshoes.ancestralarchetypes.utils;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

public class ItemUtils {
   public static NbtCompound getArchetypeTag(ItemStack stack){
      if(stack == null) return new NbtCompound();
      NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
      if(nbtComponent == null) return new NbtCompound();
      NbtCompound data = nbtComponent.copyNbt();
      if(data.contains(AncestralArchetypes.MOD_ID)){
         return data.getCompound(AncestralArchetypes.MOD_ID).orElse(new NbtCompound());
      }
      return new NbtCompound();
   }
   
   public static int getIntProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? 0 : archetypeTag.getInt(key,0);
   }
   
   public static String getStringProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? "" : archetypeTag.getString(key,"");
   }
   
   public static boolean getBooleanProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return archetypeTag == null || !archetypeTag.contains(key) ? false : archetypeTag.getBoolean(key,false);
   }
   
   public static double getDoubleProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? 0.0 : archetypeTag.getDouble(key,0.0);
   }
   
   public static float getFloatProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? 0.0f : archetypeTag.getFloat(key, 0f);
   }
   
   public static long getLongProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? 0 : archetypeTag.getLong(key, 0L);
   }
   
   public static NbtList getListProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? new NbtList() : archetypeTag.getList(key).orElse(new NbtList());
   }
   
   public static NbtCompound getCompoundProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return  archetypeTag == null || !archetypeTag.contains(key) ? new NbtCompound() : archetypeTag.getCompound(key).orElse(new NbtCompound());
   }
   
   public static void putProperty(ItemStack stack, String key, int property){
      putProperty(stack, key, NbtInt.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, boolean property){
      putProperty(stack, key, NbtByte.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, double property){
      putProperty(stack,key,NbtDouble.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, float property){
      putProperty(stack,key,NbtFloat.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, String property){
      putProperty(stack,key,NbtString.of(property));
   }
   
   public static void putProperty(ItemStack stack, String key, NbtElement property){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      archetypeTag.put(key,property);
      NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
      NbtCompound data = nbtComponent.copyNbt();
      data.put(AncestralArchetypes.MOD_ID,archetypeTag);
      NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
   }
   
   public static boolean hasProperty(ItemStack stack, String key){
      NbtCompound archetypeTag = getArchetypeTag(stack);
      return archetypeTag.contains(key);
   }
   
   public static boolean removeProperty(ItemStack stack, String key){
      if(hasProperty(stack,key)){
         NbtCompound archetypeTag = getArchetypeTag(stack);
         archetypeTag.remove(key);
         if(archetypeTag.isEmpty()){
            NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound data = nbtComponent.copyNbt();
            data.remove(AncestralArchetypes.MOD_ID);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
         }else{
            NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            NbtCompound data = nbtComponent.copyNbt();
            data.put(AncestralArchetypes.MOD_ID,archetypeTag);
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
         }
         return true;
      }
      return false;
   }
}
