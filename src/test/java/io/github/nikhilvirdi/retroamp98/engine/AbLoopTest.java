package io.github.nikhilvirdi.retroamp98.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbLoopTest {

    private AbLoop loop;

    @BeforeEach
    void setUp() {
        loop = new AbLoop();
    }

    @Test
    void testInitialState() {
        assertFalse(loop.isActive());
        assertEquals("", loop.getStatusIndicator());
        assertNull(loop.getPointA());
        assertNull(loop.getPointB());
        assertNull(loop.checkLoop(Duration.ofSeconds(10)));
    }

    @Test
    void testSetPointAOnly() {
        loop.setA(Duration.ofSeconds(5));
        assertFalse(loop.isActive());
        assertEquals("A-...", loop.getStatusIndicator());
        assertEquals(Duration.ofSeconds(5), loop.getPointA());
        assertNull(loop.getPointB());

        // Check loop does not seek before point B is set
        assertNull(loop.checkLoop(Duration.ofSeconds(10)));
    }

    @Test
    void testSetPointAAndB() {
        loop.setA(Duration.ofSeconds(5));
        loop.setB(Duration.ofSeconds(15));

        assertTrue(loop.isActive());
        assertEquals("A-B", loop.getStatusIndicator());
        assertEquals(Duration.ofSeconds(5), loop.getPointA());
        assertEquals(Duration.ofSeconds(15), loop.getPointB());

        // Position before B does not trigger seek
        assertNull(loop.checkLoop(Duration.ofSeconds(10)));

        // Position at or past B triggers seek back to A
        assertEquals(Duration.ofSeconds(5), loop.checkLoop(Duration.ofSeconds(15)));
        assertEquals(Duration.ofSeconds(5), loop.checkLoop(Duration.ofSeconds(20)));
    }

    @Test
    void testSetPointBBeforeAIsNormalized() {
        loop.setA(Duration.ofSeconds(20));
        loop.setB(Duration.ofSeconds(5));

        assertTrue(loop.isActive());
        assertEquals("A-B", loop.getStatusIndicator());
        // A should be 5s and B should be 20s
        assertEquals(Duration.ofSeconds(5), loop.getPointA());
        assertEquals(Duration.ofSeconds(20), loop.getPointB());

        assertEquals(Duration.ofSeconds(5), loop.checkLoop(Duration.ofSeconds(20)));
    }

    @Test
    void testClearLoop() {
        loop.setA(Duration.ofSeconds(5));
        loop.setB(Duration.ofSeconds(15));
        assertTrue(loop.isActive());

        loop.clear();
        assertFalse(loop.isActive());
        assertEquals("", loop.getStatusIndicator());
        assertNull(loop.getPointA());
        assertNull(loop.getPointB());
        assertNull(loop.checkLoop(Duration.ofSeconds(20)));
    }
}
