package io.github.backroomshorror;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
/** Original spindly shadow creature built from vanilla block displays; no pack required. */
final class BacteriaModels implements BacteriaVisuals {
 private record Part(double x,double y,double z,float width,float height,float depth,float bend,int limb){}
 private record Body(Mob mob,List<BlockDisplay> pieces){}
 private final Map<UUID,Body> bodies=new HashMap<>();private int ticks;
 private static final List<Part> PARTS=List.of(
  new Part(0,1.48,0,.28f,1.12f,.24f,.05f,0),new Part(0,2.62,0,.4f,.38f,.35f,-.12f,0),
  new Part(-.18,2.34,0,.55f,.12f,.16f,.12f,0),
  new Part(-.33,1.4,0,.12f,1.08f,.12f,-.22f,1),new Part(.34,1.37,0,.12f,1.15f,.12f,.28f,2),
  new Part(-.12,.16,0,.13f,1.37f,.13f,-.09f,3),new Part(.12,.12,0,.13f,1.42f,.13f,.12f,4),
  new Part(-.55,1.02,0,.08f,.48f,.08f,-.42f,1),new Part(.58,1.02,0,.08f,.48f,.08f,.48f,2),
  new Part(-.06,2.9,0,.1f,.28f,.1f,.27f,0));
 BacteriaModels(JavaPlugin plugin){plugin.getServer().getScheduler().runTaskTimer(plugin,this::tick,1,1);}
 public void add(Mob mob){List<BlockDisplay> pieces=new ArrayList<>();try{for(Part part:PARTS)pieces.add(mob.getWorld().spawn(mob.getLocation(),BlockDisplay.class,d->{d.setBlock(Material.BLACK_CONCRETE.createBlockData());d.setGravity(false);d.setPersistent(false);d.setTeleportDuration(1);d.setInterpolationDuration(1);d.setViewRange(1.2f);}));bodies.put(mob.getUniqueId(),new Body(mob,pieces));mob.setInvisible(true);}catch(RuntimeException e){pieces.forEach(Entity::remove);throw e;}}
 private void tick(){ticks++;for(Body body:new ArrayList<>(bodies.values())){if(!body.mob.isValid()||body.mob.isDead()){remove(body.mob.getUniqueId());continue;}Location origin=body.mob.getLocation();double yaw=Math.toRadians(origin.getYaw());boolean walking=body.mob.getVelocity().lengthSquared()>.005;for(int i=0;i<PARTS.size();i++){Part p=PARTS.get(i);BlockDisplay d=body.pieces.get(i);if(!d.isValid())continue;Location at=origin.clone().add(Math.cos(yaw)*p.x+Math.sin(yaw)*p.z,p.y,Math.sin(yaw)*p.x-Math.cos(yaw)*p.z);at.setPitch(0);d.teleport(at);float swing=walking&&p.limb>0?(float)Math.sin(ticks*.22+(p.limb%2)*Math.PI)*.25f:0;d.setTransformation(new Transformation(new Vector3f(-p.width/2,0,-p.depth/2),new Quaternionf().rotateZ(p.bend).rotateX(swing),new Vector3f(p.width,p.height,p.depth),new Quaternionf()));}}}
 public void remove(UUID id){Body body=bodies.remove(id);if(body!=null)body.pieces.forEach(Entity::remove);}
 public void close(){for(UUID id:new ArrayList<>(bodies.keySet()))remove(id);}
}
