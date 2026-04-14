package smhy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonExportConfigurationTest {

    // -----------------------------------------------------------------------
    // isEnabled() when commandLineOverride is null — falls back to 'enabled'
    // -----------------------------------------------------------------------

    @Test
    void isEnabledReturnsTrueWhenEnabledTrueAndNoOverride() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        config.setEnabled(true);

        assertTrue(config.isEnabled());
    }

    @Test
    void isEnabledReturnsFalseWhenEnabledFalseAndNoOverride() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        config.setEnabled(false);

        assertFalse(config.isEnabled());
    }

    // -----------------------------------------------------------------------
    // isEnabled() when commandLineOverride is set — override wins regardless
    // -----------------------------------------------------------------------

    @Test
    void isEnabledReturnsTrueWhenOverrideTrueEvenIfEnabledFalse() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        config.setEnabled(false);
        config.setCommandLineOverride(true);

        assertTrue(config.isEnabled());
    }

    @Test
    void isEnabledReturnsFalseWhenOverrideFalseEvenIfEnabledTrue() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        config.setEnabled(true);
        config.setCommandLineOverride(false);

        assertFalse(config.isEnabled());
    }

    // -----------------------------------------------------------------------
    // setCommandLineOverride(null) restores fall-through to 'enabled'
    // -----------------------------------------------------------------------

    @Test
    void isEnabledFallsBackToEnabledAfterOverrideSetToNull() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        config.setEnabled(true);
        config.setCommandLineOverride(false); // override: disabled
        assertFalse(config.isEnabled());

        config.setCommandLineOverride(null);  // remove override
        assertTrue(config.isEnabled());       // falls back to enabled=true
    }

    @Test
    void commandLineOverrideIsNullByDefault() {
        JsonExportConfiguration config = new JsonExportConfiguration();
        assertNull(config.getCommandLineOverride());
    }
}
