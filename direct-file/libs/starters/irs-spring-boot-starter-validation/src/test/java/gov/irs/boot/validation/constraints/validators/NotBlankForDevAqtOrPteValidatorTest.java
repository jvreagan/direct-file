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
class NotBlankForDevAqtOrPteValidatorTest {

    @Mock
    private Environment env;

    private NotBlankForDevAqtOrPteValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NotBlankForDevAqtOrPteValidator(env);
    }

    @Test
    void givenTargetProfile_whenBlankValue_thenReturnsFalse() {
        when(env.matchesProfiles("aws-dev | aws-aqt | aws-pte")).thenReturn(true);

        assertThat(validator.isValid("", null)).isFalse();
        assertThat(validator.isValid("   ", null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void givenTargetProfile_whenValidText_thenReturnsTrue() {
        when(env.matchesProfiles("aws-dev | aws-aqt | aws-pte")).thenReturn(true);

        assertThat(validator.isValid("some-value", null)).isTrue();
    }

    @Test
    void givenOtherProfile_whenBlankValue_thenReturnsTrue() {
        when(env.matchesProfiles("aws-dev | aws-aqt | aws-pte")).thenReturn(false);

        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void givenOtherProfile_whenValidText_thenReturnsTrue() {
        when(env.matchesProfiles("aws-dev | aws-aqt | aws-pte")).thenReturn(false);

        assertThat(validator.isValid("some-value", null)).isTrue();
    }
}
