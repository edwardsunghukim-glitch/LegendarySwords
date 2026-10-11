package io.github.legendarykoth;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CaptureStateTest {
    @Test void winsAtExactlyOneHundredUncontestedTicks() {
        CaptureState state=new CaptureState();UUID a=UUID.randomUUID();
        for(int i=0;i<99;i++)assertNull(state.tick(Set.of(a),100,true));
        assertEquals(a,state.tick(Set.of(a),100,true));assertEquals(100,state.seconds);
    }
    @Test void contestedHillPausesHolderAndResumesWhenChallengerLeaves() {
        CaptureState s=new CaptureState();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        s.tick(Set.of(a),100,true);s.tick(Set.of(a,b),100,true);
        assertTrue(s.contested);assertEquals(1,s.seconds);assertEquals(a,s.holder);
        s.tick(Set.of(a),100,true);assertEquals(2,s.seconds);
    }
    @Test void leavingResetsAndNewHolderStartsAtOne() {
        CaptureState s=new CaptureState();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        s.tick(Set.of(a),100,true);s.tick(Set.of(a),100,true);s.tick(Set.of(b),100,true);
        assertEquals(b,s.holder);assertEquals(1,s.seconds);
        s.tick(Set.of(),100,true);assertNull(s.holder);assertEquals(0,s.seconds);
    }
    @Test void configuredContestResetDoesNotPreserveProgress() {
        CaptureState s=new CaptureState();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        s.tick(Set.of(a),100,false);s.tick(Set.of(a,b),100,false);assertEquals(0,s.seconds);
    }
    @Test void nobodyGainsProgressIfHillStartsContested() {
        CaptureState s=new CaptureState();s.tick(Set.of(UUID.randomUUID(),UUID.randomUUID()),100,true);
        assertNull(s.holder);assertEquals(0,s.seconds);
    }
    @Test void stoppedEventClearsAllCaptureState() {
        CaptureState s=new CaptureState();s.tick(Set.of(UUID.randomUUID()),100,true);s.reset();
        assertNull(s.holder);assertEquals(0,s.seconds);assertFalse(s.contested);
    }
}
