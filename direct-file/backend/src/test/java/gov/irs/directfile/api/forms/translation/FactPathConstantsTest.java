package gov.irs.directfile.api.forms.translation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FactPathConstantsTest {

    @Test
    void givenAllStringConstants_whenInspected_thenAllStartWithSlash() throws Exception {
        for (Field field : FactPathConstants.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && field.getType() == String.class) {
                String value = (String) field.get(null);
                assertTrue(
                        value.startsWith("/"),
                        "Constant " + field.getName() + " should start with '/' but was: " + value);
            }
        }
    }

    @Test
    void givenFormatStringConstants_whenInspected_thenContainPercentS() throws Exception {
        int formatCount = 0;
        for (Field field : FactPathConstants.class.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && field.getType() == String.class) {
                String value = (String) field.get(null);
                if (value.contains("#")) {
                    assertTrue(
                            value.contains("%s"),
                            "Format constant " + field.getName() + " should contain '%s' but was: " + value);
                    formatCount++;
                }
            }
        }
        assertTrue(formatCount > 0, "Should have found at least one format string constant");
    }
}
