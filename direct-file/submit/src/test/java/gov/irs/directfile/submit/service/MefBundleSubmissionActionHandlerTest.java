package gov.irs.directfile.submit.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gov.irs.directfile.submit.actions.ActionContext;
import gov.irs.directfile.submit.config.Config;
import gov.irs.directfile.submit.config.DirectoriesConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MefBundleSubmissionActionHandlerTest {

    // MeF SDK expects this env variable to be defined
    @BeforeAll
    static void setupSystemProperties() {
        String userDirectory = System.getProperty("user.dir");
        System.setProperty("A2A_TOOLKIT_HOME", userDirectory + "/src/test/resources/");
    }

    @AfterAll
    static void cleanupSystemProperties() {
        System.clearProperty("A2A_TOOLKIT_HOME");
    }

    @Test
    void givenValidConfig_whenConstructed_thenNoException() {
        // given
        Config config = createConfig();
        ActionContext actionContext = new ActionContext(config);

        // when / then
        assertThatCode(() -> new MefBundleSubmissionActionHandler(config, actionContext))
                .doesNotThrowAnyException();
    }

    @Test
    void givenValidConfig_whenConstructed_thenInstanceIsNotNull() {
        // given
        Config config = createConfig();
        ActionContext actionContext = new ActionContext(config);

        // when
        MefBundleSubmissionActionHandler handler = new MefBundleSubmissionActionHandler(config, actionContext);

        // then
        assertThat(handler).isNotNull();
    }

    private Config createConfig() {
        DirectoriesConfig directoriesConfig = new DirectoriesConfig(
                "src/test/resources/test",
                "src/test/resources/test",
                "src/test/resources/test",
                "src/test/resources/test",
                "src/test/resources/test",
                "src/test/resources/test");
        return new Config(
                "Test",
                null,
                null,
                directoriesConfig,
                null,
                null,
                "12345",
                "12345",
                "12345",
                "",
                false,
                true,
                true,
                "12345",
                "12345678",
                "2023.0.1",
                "dfsys-mef-submit-deployment-0-us-gov-east-1");
    }
}
