package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.callbacks.LoginCallback;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class MetamorphTNTShieldLoginCallback extends LoginCallback {
   
   private float hearts;
   
   public MetamorphTNTShieldLoginCallback(){
      super(Identifier.fromNamespaceAndPath(AncestralArchetypes.MOD_ID, ArchetypeRegistry.METAMORPH.id()));
   }
   
   public MetamorphTNTShieldLoginCallback(ServerPlayer player, float hearts){
      this();
      this.playerUUID = player.getStringUUID();
      this.hearts = hearts;
   }
   
   @Override
   public void onLogin(ServerGamePacketListenerImpl netHandler, MinecraftServer server){
      // Double check that this is the correct player before running timer
      ServerPlayer player = netHandler.player;
      if(player.getStringUUID().equals(playerUUID)){
         float removed = Math.max(0,player.getAbsorptionAmount()-hearts);
         MinecraftUtils.removeMaxAbsorption(player, Identifier.fromNamespaceAndPath(AncestralArchetypes.MOD_ID, ArchetypeRegistry.METAMORPH.id()),hearts);
         player.setAbsorptionAmount(removed);
      }
   }
   
   @Override
   public void setData(CompoundTag data){
      //Data tag just has single float for hearts
      this.data = data;
      hearts = data.getFloatOr("hearts", 0.0f);
   }
   
   @Override
   public CompoundTag getData(){
      CompoundTag data = new CompoundTag();
      data.putFloat("hearts",hearts);
      this.data = data;
      return this.data;
   }
   
   @Override
   public boolean canCombine(LoginCallback loginCallback){
      return true;
   }
   
   @Override
   public boolean combineCallbacks(LoginCallback callback){
      if(callback instanceof MetamorphTNTShieldLoginCallback newCallback){
         this.hearts += newCallback.hearts;
         return true;
      }
      return false;
   }
   
   @Override
   public LoginCallback makeNew(){
      return new MetamorphTNTShieldLoginCallback();
   }
}