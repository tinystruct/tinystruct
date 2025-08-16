package org.tinystruct.data.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConditionTest {

    @Test
    public void testSelectWithValidTable() {
        Condition condition = new Condition();
        condition.select("users");
        assertEquals("SELECT * FROM users", condition.toString());
    }

    @Test
    public void testSelectWithInvalidTable() {
        Condition condition = new Condition();
        assertThrows(IllegalArgumentException.class, () -> condition.select(""));
    }

    @Test
    public void testAndCondition() {
        Condition condition = new Condition();
        condition.select("users").and("age > 18");
        assertEquals("SELECT * FROM users WHERE age > 18", condition.toString());
    }

    @Test
    public void testOrCondition() {
        Condition condition = new Condition();
        condition.select("users").or("name = 'John'");
        assertEquals("SELECT * FROM users WHERE name = 'john'", condition.toString());
    }

    @Test
    public void testOrderBy() {
        Condition condition = new Condition();
        condition.select("users").orderBy("age DESC");
        assertEquals("SELECT * FROM users ORDER BY age desc", condition.toString());
    }

    @Test
    public void testWithCustomSQL() {
        Condition condition = new Condition();
        condition.select("users").with("WHERE age > 18 ORDER BY name ASC");
        assertEquals("SELECT * FROM users WHERE age > 18 ORDER BY name ASC", condition.toString());
    }

    @Test
    public void testSetRequestFields() {
        Condition condition = new Condition();
        condition.setRequestFields("id, name").select("users");
        assertEquals("SELECT id, name FROM users", condition.toString());
    }
}
