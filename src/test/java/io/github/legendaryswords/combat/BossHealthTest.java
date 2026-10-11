package io.github.legendaryswords.combat;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BossHealthTest {
    @Test void addsHealthWithoutChangingBaseAndRemovesOnlyOwnedModifier() {
        Player p=mock(Player.class);AttributeInstance attr=mock(AttributeInstance.class);
        List<AttributeModifier> mods=new ArrayList<>();
        AttributeModifier other=new AttributeModifier(new NamespacedKey("other","bonus"),10,AttributeModifier.Operation.ADD_NUMBER);mods.add(other);
        when(attr.getModifiers()).thenAnswer(a->List.copyOf(mods));
        when(attr.getValue()).thenAnswer(a->20+mods.stream().mapToDouble(AttributeModifier::getAmount).sum());
        doAnswer(a->{mods.add(a.getArgument(0));return null;}).when(attr).addModifier(any(AttributeModifier.class));
        doAnswer(a->{mods.remove(a.getArgument(0));return null;}).when(attr).removeModifier(any(AttributeModifier.class));
        AtomicReference<Double> health=new AtomicReference<>(30d);when(p.getHealth()).thenAnswer(a->health.get());
        doAnswer(a->{health.set(a.getArgument(0));return null;}).when(p).setHealth(anyDouble());
        BossHealth.apply(p,attr,80);assertEquals(110,attr.getValue());assertEquals(110,health.get());
        BossHealth.remove(p,attr);assertEquals(30,attr.getValue());assertEquals(30,health.get());assertEquals(List.of(other),mods);
        verify(attr,never()).setBaseValue(anyDouble());
    }
    @Test void missingAttributeAndInvalidBonusDoNotTouchPlayerHealth() {
        Player p=mock(Player.class);AttributeInstance attr=mock(AttributeInstance.class);
        BossHealth.apply(p,null,80);BossHealth.apply(p,attr,Double.NaN);BossHealth.apply(p,attr,-1);
        verify(p,never()).setHealth(anyDouble());verify(attr,never()).addModifier(any(AttributeModifier.class));
    }
    @Test void deadPlayerIsNeverRevivedByCleanup() {
        Player p=mock(Player.class);AttributeInstance attr=mock(AttributeInstance.class);when(p.isDead()).thenReturn(true);when(attr.getModifiers()).thenReturn(List.of());
        BossHealth.remove(p,attr);verify(p,never()).setHealth(anyDouble());
    }
}
