package io.github.legendaryarmor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PassiveCleanupTest {
    @Test void armorModifierDoesNotStackAndCleanupPreservesOtherPlugins() {
        AttributeInstance instance=mock(AttributeInstance.class);Set<AttributeModifier> modifiers=new HashSet<>();
        var other=new AttributeModifier(new NamespacedKey("anotherplugin","speed"),.2,AttributeModifier.Operation.ADD_SCALAR);modifiers.add(other);
        when(instance.getModifiers()).thenAnswer(call->List.copyOf(modifiers));
        doAnswer(call->{modifiers.add(call.getArgument(0));return null;}).when(instance).addModifier(any());
        doAnswer(call->{modifiers.remove(call.getArgument(0));return null;}).when(instance).removeModifier(any(AttributeModifier.class));
        var key=new NamespacedKey("legendaryarmor","set_speed");
        LegendaryArmor.updateModifier(instance,key,.08,AttributeModifier.Operation.ADD_SCALAR);
        LegendaryArmor.updateModifier(instance,key,.08,AttributeModifier.Operation.ADD_SCALAR);assertEquals(2,modifiers.size());
        LegendaryArmor.updateModifier(instance,key,0,AttributeModifier.Operation.ADD_SCALAR);assertEquals(Set.of(other),modifiers);
        LegendaryArmor.updateModifier(null,key,0,AttributeModifier.Operation.ADD_SCALAR);
    }
}
