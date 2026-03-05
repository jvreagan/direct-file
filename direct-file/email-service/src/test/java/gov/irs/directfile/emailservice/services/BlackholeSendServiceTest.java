package gov.irs.directfile.emailservice.services;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.emailservice.domain.SendEmail;
import gov.irs.directfile.models.email.HtmlTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BlackholeSendServiceTest {

    private BlackholeSendService blackholeSendService;

    @BeforeEach
    void setUp() {
        blackholeSendService = new BlackholeSendService();
    }

    @Test
    void givenValidEmail_whenSendEmail_thenReturnsTrue() {
        SendEmail email = new SendEmail(
                UUID.randomUUID(),
                "submissionId",
                UUID.randomUUID(),
                "user@example.com",
                null,
                "en",
                HtmlTemplate.SUBMITTED);

        boolean result = blackholeSendService.sendEmail(email);

        assertThat(result).isTrue();
    }

    @Test
    void givenAnyEmail_whenSendEmail_thenAlwaysReturnsTrue() {
        SendEmail email = new SendEmail(
                UUID.randomUUID(),
                "anotherSubmissionId",
                UUID.randomUUID(),
                "different-user@example.com",
                null,
                "es",
                HtmlTemplate.ACCEPTED);

        boolean result = blackholeSendService.sendEmail(email);

        assertThat(result).isTrue();
    }
}
