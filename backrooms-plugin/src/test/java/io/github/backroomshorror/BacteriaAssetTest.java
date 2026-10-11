package io.github.backroomshorror;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import kr.toxicity.model.api.BetterModelConfig;
import kr.toxicity.model.api.data.raw.*;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;
class BacteriaAssetTest {
 @Test void uploadedMeshKeepsEveryTriangleAndSixClipsAndLoadsInBetterModel()throws Exception{
  var platform=mock(BetterModelPlatform.class,RETURNS_DEEP_STUBS);when(platform.config()).thenReturn(mock(BetterModelConfig.class,RETURNS_DEEP_STUBS));BetterModel.register(platform);
  try(var input=getClass().getResourceAsStream("/bettermodel/backroomshorror_bacteria.bbmodel")){
   assertNotNull(input);
   ModelData data=ModelData.GSON.fromJson(new InputStreamReader(input),ModelData.class);
   assertEquals(26478,data.elements().stream().mapToInt(e->((ModelElement.Mesh)e).faces().size()).sum());assertEquals(23,data.groups().size());assertEquals(Set.of("idle","walk","run","charge","scream","claw"),data.animations().stream().map(ModelAnimation::name).collect(Collectors.toSet()));
   var result=data.loadBlueprint("backroomshorror_bacteria",true);assertTrue(result.errors().isEmpty(),result.errors().toString());assertNotNull(result.blueprint());
  }
 }
}
