package org.tinystruct.system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinystruct.AbstractApplication;
import org.tinystruct.application.ActionRegistry;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Argument;
import org.tinystruct.system.cli.CommandArgument;
import org.tinystruct.system.cli.CommandLine;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The command metadata built from {@code @Action(arguments = ...)} keeps the declaration order,
 * the optional flag and the Java parameter type (as the argument's type), and string arguments
 * convert to collections of enums.
 */
class ActionArgumentMetadataTest {

    public enum Role { ADMIN, USER, AUDITOR }

    public static class MetadataApp extends AbstractApplication {
        @Override
        public void init() {
            setTemplateRequired(false);
        }

        @Override
        public String version() {
            return "1.0";
        }

        @Action(value = "meta/create",
                description = "Create something.",
                arguments = {
                        @Argument(key = "name", description = "The name"),
                        @Argument(key = "role", description = "The role"),
                        @Argument(key = "notify", type = "boolean", description = "Notify?", optional = true),
                        @Argument(key = "count", type = "number", description = "How many")
                })
        public String create(String name, Role role, boolean notify, int count) {
            return name + ":" + role + ":" + notify + ":" + count;
        }

        @Action(value = "meta/assign",
                description = "Assign roles.",
                arguments = {
                        @Argument(key = "name", description = "The name"),
                        @Argument(key = "roles", description = "The roles")
                })
        public String assign(String name, Set<Role> roles) {
            return name + ":" + roles;
        }

        @Action(value = "meta/tag",
                description = "Tag with roles.",
                arguments = {
                        @Argument(key = "roles", description = "The roles")
                })
        public String tag(List<Role> roles) {
            return roles.getClass().getSimpleName() + ":" + roles;
        }
    }

    private static MetadataApp app;

    @BeforeAll
    static void install() {
        Settings settings = new Settings();
        app = new MetadataApp();
        ApplicationManager.install(app, settings);
    }

    @Test
    void argumentsKeepDeclarationOrderOptionalAndType() {
        CommandLine command = ActionRegistry.getInstance().getCommand("meta/create");
        assertNotNull(command);

        List<CommandArgument<String, Object>> args = new ArrayList<>(command.getArguments());
        assertEquals(List.of("name", "role", "notify", "count"),
                args.stream().map(CommandArgument::getKey).toList());

        assertFalse(args.get(0).isOptional());
        assertTrue(args.get(2).isOptional());
        assertEquals(String.class, args.get(0).getType());
        assertEquals(Role.class, args.get(1).getType());
        assertEquals(boolean.class, args.get(2).getType());
        assertEquals(int.class, args.get(3).getType());
        assertEquals("The role", args.get(1).getDescription());
    }

    @Test
    void genericParameterTypeIsPreserved() {
        CommandLine command = ActionRegistry.getInstance().getCommand("meta/assign");
        List<CommandArgument<String, Object>> args = new ArrayList<>(command.getArguments());

        assertTrue(args.get(1).getType() instanceof ParameterizedType);
        ParameterizedType type = (ParameterizedType) args.get(1).getType();
        assertEquals(Set.class, type.getRawType());
        assertEquals(Role.class, type.getActualTypeArguments()[0]);
    }

    @Test
    void commaSeparatedEnumsConvertToASet() throws Exception {
        Object result = app.invoke("meta/assign/John/ADMIN,USER");
        assertEquals("John:[ADMIN, USER]", result);
    }

    @Test
    void commaSeparatedEnumsConvertToAList() throws Exception {
        Object result = app.invoke("meta/tag/AUDITOR,ADMIN");
        assertEquals("ArrayList:[AUDITOR, ADMIN]", result);
    }

    @Test
    void unknownEnumConstantInACollectionFails() {
        assertThrows(Exception.class, () -> app.invoke("meta/assign/John/ADMIN,NOPE"));
    }

    @Test
    void scalarConversionsStillWork() throws Exception {
        assertEquals("Ann:USER:true:3", app.invoke("meta/create/Ann/USER/true/3"));
    }
}
