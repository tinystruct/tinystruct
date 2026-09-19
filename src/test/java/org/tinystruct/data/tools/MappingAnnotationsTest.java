package org.tinystruct.data.tools;

import org.junit.jupiter.api.Test;
import org.tinystruct.ApplicationRuntimeException;
import org.tinystruct.dom.Element;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MappingAnnotationsTest {
    private static final String NL = "\n";

    private static Element classElement(String table) {
        Element root = new Element("mapping");
        Element clazz = root.addElement("class");
        clazz.setAttribute("name", "Widget");
        clazz.setAttribute("table", table);
        return clazz;
    }

    private static void id(Element clazz, String column, String type, String length, boolean increment) {
        Element id = clazz.addElement("id");
        id.setAttribute("name", "Id");
        id.setAttribute("column", column);
        id.setAttribute("increment", String.valueOf(increment));
        id.setAttribute("generate", String.valueOf(!increment));
        id.setAttribute("length", length);
        id.setAttribute("type", type);
    }

    private static void property(Element clazz, String name, String column, String type, String length) {
        Element property = clazz.addElement("property");
        property.setAttribute("name", name);
        property.setAttribute("column", column);
        property.setAttribute("length", length);
        property.setAttribute("type", type);
    }

    @Test
    public void testXmlModeChangesNothing() {
        Element clazz = classElement("widgets");
        property(clazz, "name", "name", "varchar", "255");
        StringBuilder members = new StringBuilder("\tprivate String name;" + NL);
        Set<String> imports = new TreeSet<>();

        String annotation = MappingAnnotations.apply(MappingMode.XML, clazz, members, imports, NL);

        assertEquals("", annotation);
        assertEquals("\tprivate String name;" + NL, members.toString());
        assertTrue(imports.isEmpty());
    }

    @Test
    public void testTableIdAndColumns() {
        Element clazz = classElement("widgets");
        id(clazz, "id", "int", "11", true);
        property(clazz, "name", "name", "varchar", "255");
        property(clazz, "userName", "user_name", "varchar", "64");
        StringBuilder members = new StringBuilder()
                .append("\tprivate String name;").append(NL)
                .append("\tprivate String userName;").append(NL);
        Set<String> imports = new TreeSet<>();

        String annotation = MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, members, imports, NL);

        assertEquals("@Table(name = \"widgets\"," + NL
                + "        id = @Id(name = \"Id\", column = \"id\", type = \"int\", length = 11, increment = true))" + NL,
                annotation);
        assertEquals("\t@Column(name = \"name\", type = \"varchar\", length = 255)" + NL
                + "\tprivate String name;" + NL
                + "\t@Column(name = \"user_name\", type = \"varchar\", length = 64)" + NL
                + "\tprivate String userName;" + NL, members.toString());
        assertEquals(Set.of("org.tinystruct.data.annotation.Table",
                "org.tinystruct.data.annotation.Id",
                "org.tinystruct.data.annotation.Column"), imports);
    }

    @Test
    public void testGeneratedIdAndNoIncrement() {
        Element clazz = classElement("sessions");
        id(clazz, "id", "varchar", "36", false);

        String annotation = MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, new StringBuilder(),
                new TreeSet<>(), NL);

        assertTrue(annotation.contains("generate = true"), annotation);
        assertFalse(annotation.contains("increment"), annotation);
    }

    @Test
    public void testTableWithoutIdentifierOrColumns() {
        Set<String> imports = new TreeSet<>();

        String annotation = MappingAnnotations.apply(MappingMode.ANNOTATION, classElement("logs"),
                new StringBuilder(), imports, NL);

        assertEquals("@Table(name = \"logs\")" + NL, annotation);
        assertEquals(Set.of("org.tinystruct.data.annotation.Table"), imports);
    }

    @Test
    public void testLengthThatIsNotAPlainIntegerIsOmitted() {
        // e.g. decimal(10,2) is recorded as "10,2" in the XML, which the runtime reads as 0.
        Element clazz = classElement("prices");
        property(clazz, "amount", "amount", "decimal", "10,2");
        property(clazz, "note", "note", "text", "0");
        StringBuilder members = new StringBuilder()
                .append("\tprivate double amount;").append(NL)
                .append("\tprivate String note;").append(NL);

        MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, members, new TreeSet<>(), NL);

        assertTrue(members.toString().contains("@Column(name = \"amount\", type = \"decimal\")" + NL));
        assertTrue(members.toString().contains("@Column(name = \"note\", type = \"text\")" + NL));
    }

    @Test
    public void testNamesAreEscaped() {
        Element clazz = classElement("we\"ird\\table");

        String annotation = MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, new StringBuilder(),
                new TreeSet<>(), NL);

        assertEquals("@Table(name = \"we\\\"ird\\\\table\")" + NL, annotation);
    }

    @Test
    public void testSimilarPropertyNamesAreAnnotatedIndependently() {
        Element clazz = classElement("widgets");
        property(clazz, "name", "name", "varchar", "10");
        StringBuilder members = new StringBuilder()
                .append("\tprivate String userName;").append(NL)
                .append("\tprivate String name;").append(NL);

        MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, members, new TreeSet<>(), NL);

        assertEquals("\tprivate String userName;" + NL
                + "\t@Column(name = \"name\", type = \"varchar\", length = 10)" + NL
                + "\tprivate String name;" + NL, members.toString());
    }

    @Test
    public void testMissingMemberDeclarationIsAnError() {
        Element clazz = classElement("widgets");
        property(clazz, "name", "name", "varchar", "10");

        assertThrows(ApplicationRuntimeException.class, () ->
                MappingAnnotations.apply(MappingMode.ANNOTATION, clazz, new StringBuilder(), new TreeSet<>(), NL));
    }
}
