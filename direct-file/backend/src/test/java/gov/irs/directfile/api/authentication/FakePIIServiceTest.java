package gov.irs.directfile.api.authentication;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FakePIIServiceTest {

    private FakePIIService fakePIIService;

    @BeforeEach
    void setUp() {
        fakePIIService = new FakePIIService();
    }

    @Test
    void givenEmailAttribute_whenFetchAttributes_thenReturnsFormattedEmail() {
        UUID userId = UUID.randomUUID();
        Set<PIIAttribute> attributes = Set.of(PIIAttribute.EMAILADDRESS);

        Map<PIIAttribute, String> result = fakePIIService.fetchAttributes(userId, attributes);

        assertThat(result).containsKey(PIIAttribute.EMAILADDRESS);
        String email = result.get(PIIAttribute.EMAILADDRESS);
        assertThat(email).isEqualTo(String.format("test-user+%s@directfile.test", userId.toString()));
        assertThat(email).contains(userId.toString());
        assertThat(email).startsWith("test-user+");
        assertThat(email).endsWith("@directfile.test");
    }

    @Test
    void givenTinAttribute_whenFetchAttributes_thenReturnsFakeTin() {
        UUID userId = UUID.randomUUID();
        Set<PIIAttribute> attributes = Set.of(PIIAttribute.TIN);

        Map<PIIAttribute, String> result = fakePIIService.fetchAttributes(userId, attributes);

        assertThat(result).containsKey(PIIAttribute.TIN);
        assertThat(result.get(PIIAttribute.TIN)).isEqualTo("123001234");
        assertThat(result.get(PIIAttribute.TIN)).isEqualTo(FakePIIService.TIN);
    }

    @Test
    void givenOtherAttribute_whenFetchAttributes_thenReturnsPlaceholder() {
        UUID userId = UUID.randomUUID();
        Set<PIIAttribute> attributes = Set.of(PIIAttribute.GIVENNAME, PIIAttribute.SURNAME, PIIAttribute.DATEOFBIRTH);

        Map<PIIAttribute, String> result = fakePIIService.fetchAttributes(userId, attributes);

        assertThat(result).hasSize(3);
        assertThat(result.get(PIIAttribute.GIVENNAME)).isEqualTo("FAKE_PII_PLACEHOLDER");
        assertThat(result.get(PIIAttribute.SURNAME)).isEqualTo("FAKE_PII_PLACEHOLDER");
        assertThat(result.get(PIIAttribute.DATEOFBIRTH)).isEqualTo("FAKE_PII_PLACEHOLDER");
    }
}
