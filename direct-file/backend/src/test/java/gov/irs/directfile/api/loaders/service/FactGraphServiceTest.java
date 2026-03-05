package gov.irs.directfile.api.loaders.service;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import gov.irs.factgraph.FactDictionary;

import gov.irs.directfile.api.loaders.domain.GraphGetResult;
import gov.irs.directfile.api.loaders.domain.TaxDictionaryDigest;
import gov.irs.directfile.models.FactTypeWithItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class FactGraphServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    private ObjectMapper objectMapper;

    private FactGraphService factGraphService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        factGraphService = new FactGraphService(objectMapper, applicationContext);

        // Bypass @PostConstruct init() by manually setting required static fields
        // so that tests can run without Spring context or XML loading.
        ReflectionTestUtils.setField(FactGraphService.class, "_factDictionary", new FactDictionary());
        ReflectionTestUtils.setField(FactGraphService.class, "_digest", new TaxDictionaryDigest("test", Map.of()));
    }

    @Test
    void givenEmptyFacts_whenFactsParseCorrectly_thenReturnsBooleanWithoutException() {
        Map<String, FactTypeWithItem> emptyMap = new HashMap<>();

        assertThatCode(() -> {
                    boolean result = factGraphService.factsParseCorrectly(emptyMap);
                    assertThat(result).isInstanceOf(Boolean.class);
                })
                .doesNotThrowAnyException();
    }

    @Test
    void givenNullFacts_whenFactsParseCorrectly_thenReturnsFalse() {
        boolean result = factGraphService.factsParseCorrectly(null);

        assertThat(result).isFalse();
    }

    @Test
    void givenMissingKey_whenGetFact_thenResultHasError() {
        Map<String, FactTypeWithItem> emptyMap = new HashMap<>();
        String nonExistentPath = "/nonExistent/path";

        GraphGetResult result = factGraphService.getFact(emptyMap, nonExistentPath);

        assertThat(result).isNotNull();
        assertThat(result.path()).isEqualTo(nonExistentPath);
        // With an empty fact dictionary and empty persister state,
        // the fact path will not resolve, so either value is null or there is an error.
        assertThat(result.value() == null || result.hasError()).isTrue();
    }

    @Test
    void givenService_whenGetDigest_thenReturnsNonNullDigest() {
        TaxDictionaryDigest digest = factGraphService.getDigest();

        assertThat(digest).isNotNull();
        assertThat(digest.getSourceName()).isEqualTo("test");
    }
}
