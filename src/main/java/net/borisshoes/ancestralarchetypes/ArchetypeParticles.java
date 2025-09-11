package net.borisshoes.ancestralarchetypes;

import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;

public class ArchetypeParticles extends ParticleEffectUtils {
   public static void guardianRay(ServerWorld world, Vec3d p1, Vec3d p2, int tick){
      int windup = CONFIG.getInt(ArchetypeRegistry.GUARDIAN_RAY_WINDUP);
      double length = p2.subtract(p1).length();
      Vec3d basisP1 = Vec3d.ZERO;
      Vec3d basisP2 = new Vec3d(0,length,0);
      
      Vec3d basis = basisP2.subtract(basisP1);
      Vec3d newBasis = p2.subtract(p1);
      Quaternionf transform = new Quaternionf().rotationTo(basis.toVector3f(), newBasis.toVector3f());
      
      int intervals = (int)(basisP1.subtract(basisP2).length()*(6+tick%2));
      double delta = 0.02;
      double radius = 0.2;
      double dy = (basisP2.y-basisP1.y)/intervals;
      for(int i = 0; i < intervals; i++){
         double y = basisP1.y + dy*i;
         double theta = i * Math.PI / 12.0 + tick * Math.PI / 12.0;
         Vec3d centerPos = new Vec3d(0,y,0);
         Vec3d helixPos1 = new Vec3d(radius*Math.cos(theta),y,radius*Math.sin(theta));
         Vec3d helixPos2 = new Vec3d(radius*Math.cos(theta+Math.PI),y,radius*Math.sin(theta+Math.PI));
         Vec3d centerPosAdjust = new Vec3d(transform.transform(centerPos.toVector3f())).add(p1);
         Vec3d helixPosAdjust1 = new Vec3d(transform.transform(helixPos1.toVector3f())).add(p1);
         Vec3d helixPosAdjust2 = new Vec3d(transform.transform(helixPos2.toVector3f())).add(p1);
         
         if(tick % 2 == 0){
            if(tick < windup){
               int rgb1 = 0x6116b8;
               int rgb2 = 0xe4f00e;
               
               int r1 = (rgb1 >> 16) & 0xFF;
               int g1 = (rgb1 >> 8) & 0xFF;
               int b1 = rgb1 & 0xFF;
               int r2 = (rgb2 >> 16) & 0xFF;
               int g2 = (rgb2 >> 8) & 0xFF;
               int b2 = rgb2 & 0xFF;
               float ratio = (float) tick / windup;
               int r = (int) (r1 + ratio * (r2 - r1));
               int g = (int) (g1 + ratio * (g2 - g1));
               int b = (int) (b1 + ratio * (b2 - b1));
               int rgb3 = (r << 16) | (g << 8) | b;
               
               DustParticleEffect dust = new DustParticleEffect(rgb3,0.25f+1.75f*((float) tick / windup));
               spawnLongParticle(world,dust,centerPosAdjust.x,centerPosAdjust.y,centerPosAdjust.z,delta,delta,delta,0,1);
            }else{
               int r = 20;
               int g = 190+(tick % 40);
               int b = 30+2*(tick+20 % 40);
               DustParticleEffect dust1 = new DustParticleEffect((r << 16) | (g << 8) | b,1);
               DustParticleEffect dust2 = new DustParticleEffect((r << 16) | (g-100 << 8) | b-15,0.6f);
               
               spawnLongParticle(world,dust1,centerPosAdjust.x,centerPosAdjust.y,centerPosAdjust.z,delta,delta,delta,0,1);
               spawnLongParticle(world,dust2,helixPosAdjust1.x,helixPosAdjust1.y,helixPosAdjust1.z,0,0,0,0,1);
               spawnLongParticle(world,dust2,helixPosAdjust2.x,helixPosAdjust2.y,helixPosAdjust2.z,0,0,0,0,1);
            }
         }
      }
   }
}
