package io.resana;

import org.junit.Test;

import static junit.framework.Assert.*;

public class OtherTests {
    @Test
    public void testShifting() {
        int i = 0b0100010001;
        assertEquals(273, i);
        assertTrue((i & 1) != 0);
        assertFalse((i >> 1 & 1) != 0);
        assertFalse((i >> 2 & 1) != 0);
        assertFalse((i >> 3 & 1) != 0);
        assertTrue((i >> 4 & 1) != 0);
        assertFalse((i >> 5 & 1) != 0);
        assertFalse((i >> 6 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
        assertTrue((i >> 8 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
        assertFalse((i >> 7 & 1) != 0);
    }
}
