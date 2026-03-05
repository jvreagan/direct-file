package gov.irs.directfile.emailservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
class SmtpEmailSendServiceTest {

    @Test
    void givenValidConfig_whenConstructed_thenNoException() {
        assertThatNoException()
                .isThrownBy(() ->
                        new SmtpEmailSendService("smtp.example.com", 587, "user", "pass", "from@example.com", true));
    }

    @Test
    void givenBlankUsername_whenConstructed_thenNoAuthSession() {
        assertThatNoException()
                .isThrownBy(() -> new SmtpEmailSendService("smtp.example.com", 587, "", "", "from@example.com", false));
    }

    @Test
    void givenAuthConfig_whenConstructed_thenAuthEnabled() {
        assertThatNoException()
                .isThrownBy(
                        () -> new SmtpEmailSendService("localhost", 25, "admin", "secret", "noreply@irs.gov", true));
    }
}
