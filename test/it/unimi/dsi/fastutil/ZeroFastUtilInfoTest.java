package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ZeroFastUtilInfoTest {

	@Test
	public void testCoordinatesAndMarker() {
		assertEquals("io.github.lionblazer:zerofastutil", ZeroFastUtilInfo.coordinates());
		assertTrue(ZeroFastUtilInfo.marker().contains("ZeroFastUtil"));
		assertTrue(ZeroFastUtilInfo.marker().contains("io.github.lionblazer:zerofastutil"));
	}
}
