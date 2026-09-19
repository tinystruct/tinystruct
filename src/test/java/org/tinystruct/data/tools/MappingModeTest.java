package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MappingModeTest {

    @Test
    public void testDefaultsToXml() {
        assertEquals(MappingMode.XML, MappingMode.parse(null));
        assertEquals(MappingMode.XML, MappingMode.parse(""));
        assertEquals(MappingMode.XML, MappingMode.parse("  "));
        assertEquals(MappingMode.XML, MappingMode.parse("xml"));
    }

    @Test
    public void testParsesAnnotationCaseInsensitively() {
        assertEquals(MappingMode.ANNOTATION, MappingMode.parse("annotation"));
        assertEquals(MappingMode.ANNOTATION, MappingMode.parse(" Annotation "));
        assertEquals(MappingMode.ANNOTATION, MappingMode.parse("ANNOTATIONS"));
    }

    @Test
    public void testRejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> MappingMode.parse("yaml"));
    }
}
