package io.github.legendarybosses;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import kr.toxicity.model.api.data.Float3;
import kr.toxicity.model.api.data.blueprint.*;
import java.util.List;
class FullModelBoundsTest {
 private BlueprintElement.Cube cube(Float3 from,Float3 to,Float3 rotation){var cube=mock(BlueprintElement.Cube.class);when(cube.from()).thenReturn(from);when(cube.to()).thenReturn(to);when(cube.origin()).thenReturn(Float3.ZERO);when(cube.rotation()).thenReturn(rotation);when(cube.visibility()).thenReturn(true);return cube;}
 private BlueprintElement.Group group(BlueprintElement... cubes){var group=mock(BlueprintElement.Group.class);when(group.origin()).thenReturn(Float3.ZERO);when(group.children()).thenReturn(List.of(cubes));return group;}
 @Test void fullBodyIncludesEveryCubeInsteadOfThinLargestPanel(){var plate=cube(new Float3(-16,0,-8),new Float3(16,1,8),Float3.ZERO);var feet=cube(new Float3(-4,-32,-4),new Float3(4,-16,4),Float3.ZERO);var head=cube(new Float3(-4,32,-4),new Float3(4,48,4),Float3.ZERO);var box=FullModelBounds.bounds(group(plate,feet,head),0);assertEquals(2,box.x(),.0001);assertEquals(5,box.y(),.0001);assertEquals(1,box.z(),.0001);assertEquals(-2,box.minY(),.0001);assertEquals(3,box.maxY(),.0001);}
 @Test void rotatedCubesHaveAllCornersIncluded(){var cube=cube(new Float3(0,0,0),new Float3(32,4,4),new Float3(0,0,90));var box=FullModelBounds.bounds(group(cube),0);assertEquals(.25,box.x(),.0001);assertEquals(2,box.y(),.0001);assertEquals(.25,box.z(),.0001);}
 @Test void paddingInflationAndHiddenGeometryAreHandled(){var cube=cube(new Float3(-8,-8,-8),new Float3(8,8,8),Float3.ZERO);when(cube.inflate()).thenReturn(1f);var hidden=cube(new Float3(-100,-100,-100),new Float3(100,100,100),Float3.ZERO);when(hidden.visibility()).thenReturn(false);var box=FullModelBounds.bounds(group(cube,hidden),.1);assertEquals(1.325,box.x(),.0001);assertNull(FullModelBounds.bounds(group(hidden),0));}
 @Test void encounterCopyPreservesVisualItemsAndLeavesSharedBoundsUntouched(){
  try(var engine=mockStatic(kr.toxicity.model.api.BetterModel.class)){
   engine.when(kr.toxicity.model.api.BetterModel::platform).thenReturn(mock(kr.toxicity.model.api.BetterModelPlatform.class,RETURNS_DEEP_STUBS));
   var geometry=group(cube(new Float3(-8,0,-8),new Float3(8,80,8),Float3.ZERO));when(geometry.rotation()).thenReturn(Float3.ZERO);
   var original=mock(kr.toxicity.model.api.data.renderer.RendererGroup.class);
   when(original.getParent()).thenReturn(geometry);when(original.getPosition()).thenReturn(new org.joml.Vector3f());when(original.getChildren()).thenReturn(new java.util.LinkedHashMap<>());
   var item=mock(kr.toxicity.model.api.util.TransformedItemStack.class);when(original.getItemStack()).thenReturn(item);when(item.copy()).thenReturn(item);
   when(original.getItemMapper()).thenReturn(mock(kr.toxicity.model.api.bone.BoneItemMapper.class));when(original.getMountController()).thenReturn(mock(kr.toxicity.model.api.mount.MountController.class));
   var thin=ModelBoundingBox.of(1,.1,1);when(original.getHitBox()).thenReturn(thin);
   var groups=new java.util.LinkedHashMap<kr.toxicity.model.api.bone.BoneName,kr.toxicity.model.api.data.renderer.RendererGroup>();groups.put(null,original);
   var renderer=new kr.toxicity.model.api.data.renderer.ModelRenderer("test",kr.toxicity.model.api.data.renderer.ModelRenderer.Type.GENERAL,groups,java.util.Map.of());
   var copy=FullModelBounds.copy(renderer,0);var corrected=copy.rendererGroups().get(null);
   assertEquals(5,corrected.getHitBox().y(),.0001);assertSame(item,corrected.getItemStack());assertSame(thin,original.getHitBox());assertSame(renderer.animations(),copy.animations());assertNotSame(groups,copy.rendererGroups());
  }
 }
}
