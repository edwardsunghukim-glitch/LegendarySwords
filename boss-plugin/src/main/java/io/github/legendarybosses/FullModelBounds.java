package io.github.legendarybosses;

import java.util.*;
import org.joml.Vector3f;
import kr.toxicity.model.api.bone.BoneName;
import kr.toxicity.model.api.data.blueprint.*;
import kr.toxicity.model.api.data.renderer.*;

/** Uses complete part geometry instead of BetterModel's largest individual cube. */
final class FullModelBounds {
 static ModelRenderer copy(ModelRenderer original,double padding){
  return new ModelRenderer(original.name(),original.type(),groups(original.rendererGroups(),padding),original.animations());
 }
 private static SequencedMap<BoneName,RendererGroup> groups(SequencedMap<BoneName,RendererGroup> originals,double padding){
  SequencedMap<BoneName,RendererGroup> result=new LinkedHashMap<>();
  originals.forEach((name,group)->{
   ModelBoundingBox box=group.getParent() instanceof BlueprintElement.Group geometry?bounds(geometry,padding):group.getHitBox();
   if(box==null)box=group.getHitBox();
   result.put(name,new RendererGroup(group.getParent(),new Vector3f(group.getPosition()),group.getParent().rotation().toVector(),group.getItemStack(),groups(group.getChildren(),padding),box,box==null?new Vector3f():box.centerPoint(),group.getItemMapper(),group.getMountController()));
  });return result;
 }
 static ModelBoundingBox bounds(BlueprintElement.Group group,double padding){
  double[] lo={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},hi={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};boolean found=false;
  for(BlueprintElement element:group.children())if(element instanceof BlueprintElement.Cube cube&&cube.visibility()){
   found=true;Vector3f pivot=cube.origin().toVector(),origin=group.origin().toVector();var rotation=cube.rotation().toQuaternionZYX();
   float inflation=cube.inflate();
   for(int corner=0;corner<8;corner++){
    Vector3f point=new Vector3f((corner&1)==0?cube.from().x()-inflation:cube.to().x()+inflation,(corner&2)==0?cube.from().y()-inflation:cube.to().y()+inflation,(corner&4)==0?cube.from().z()-inflation:cube.to().z()+inflation);
    point.sub(pivot).rotate(rotation).add(pivot).sub(origin).div(16);point.x=-point.x;point.z=-point.z;
    double[] coords={point.x,point.y,point.z};for(int axis=0;axis<3;axis++){lo[axis]=Math.min(lo[axis],coords[axis]);hi[axis]=Math.max(hi[axis],coords[axis]);}
   }
  }
  if(!found)return null;double p=BossRules.bounded(padding,.08,0,.5);
  return ModelBoundingBox.of(lo[0]-p,lo[1]-p,lo[2]-p,hi[0]+p,hi[1]+p,hi[2]+p);
 }
 private FullModelBounds(){}
}
