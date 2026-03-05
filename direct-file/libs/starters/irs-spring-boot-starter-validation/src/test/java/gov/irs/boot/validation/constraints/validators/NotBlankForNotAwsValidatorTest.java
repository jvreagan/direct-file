package gov.irs.boot.validation.constraints.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotBlankForNotAwsValidatorTest {

    @Mock
    private Environment env;

    private NotBlankForNotAwsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NotBlankForNotAwsValidator(env);
    }

    @Test
    void givenAwsProfile_whenBlankValue_thenReturnsTrue() {
        when(env.matchesProfiles("aws")).thenReturn(true);

        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void givenAwsProfile_whenValidText_thenReturnsTrue() {
        when(env.matchesProfiles("aws")).thenReturn(true);

        assertThat(validator.isValid("some-value", null)).isTrue();
    }

    @Test
    void givenNonAwsProfile_whenBlankValue_thenReturnsFalse() {
        when(env.matchesProfiles("aws")).thenReturn(false);

        assertThat(validator.isValid("", null)).isFalse();
        assertThat(validator.isValid("   ", null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void givenNonAwsProfile_whenValidText_thenReturnsTrue() {
        when(env.matchesProfiles("aws")).thenReturn(false);

        assertThat(validator.isValid("some-value", null)).isTrue();
    }
}
