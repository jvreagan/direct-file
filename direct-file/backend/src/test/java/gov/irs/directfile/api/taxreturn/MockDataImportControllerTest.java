package gov.irs.directfile.api.taxreturn;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gov.irs.directfile.api.dataimport.MockDataImportService;
import gov.irs.directfile.api.dataimport.model.WrappedPopulatedData;
import gov.irs.directfile.api.dataimport.model.WrappedPopulatedDataNode;
import gov.irs.directfile.api.pdf.PdfService;
import gov.irs.directfile.api.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockDataImportControllerTest {

    @Mock
    private TaxReturnService taxReturnService;

    @Mock
    private UserService userService;

    @Mock
    private PdfService pdfService;

    @Mock
    private EncryptionCacheWarmingService cacheWarmingService;

    @Mock
    private MockDataImportService mockDataImportService;

    @Mock
    private HttpServletRequest request;

    private MockDataImportController controller;

    @BeforeEach
    void setUp() {
        // MockDataImportController casts the DataImportService to MockDataImportService
        // in its constructor, so we pass the mock directly.
        controller = new MockDataImportController(
                taxReturnService,
                userService,
                pdfService,
                cacheWarmingService,
                mockDataImportService);

        // Inject the mocked HttpServletRequest via the @Autowired field
        try {
            var requestField = MockDataImportController.class.getDeclaredField("request");
            requestField.setAccessible(true);
            requestField.set(controller, request);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock HttpServletRequest", e);
        }
    }

    @Test
    void givenProfileAndDobHeaders_whenGetPopulatedData_thenDelegatesToMockServiceWithHeaders() {
        // given
        UUID taxReturnId = UUID.randomUUID();
        String profile = "single-w2";
        String dob = "1990-01-15";

        when(request.getHeader("x-data-import-profile")).thenReturn(profile);
        when(request.getHeader("x-data-import-dob")).thenReturn(dob);

        WrappedPopulatedData expectedData = buildWrappedPopulatedData();
        when(mockDataImportService.getPopulatedData(profile, dob)).thenReturn(expectedData);

        // when
        WrappedPopulatedData result = controller.getPopulatedData(taxReturnId);

        // then
        assertThat(result).isSameAs(expectedData);
        verify(mockDataImportService).getPopulatedData(profile, dob);
    }

    @Test
    void givenNullHeaders_whenGetPopulatedData_thenDelegatesToMockServiceWithNulls() {
        // given
        UUID taxReturnId = UUID.randomUUID();

        when(request.getHeader("x-data-import-profile")).thenReturn(null);
        when(request.getHeader("x-data-import-dob")).thenReturn(null);

        WrappedPopulatedData expectedData = buildWrappedPopulatedData();
        when(mockDataImportService.getPopulatedData(null, null)).thenReturn(expectedData);

        // when
        WrappedPopulatedData result = controller.getPopulatedData(taxReturnId);

        // then
        assertThat(result).isSameAs(expectedData);
        verify(mockDataImportService).getPopulatedData(null, null);
    }

    @Test
    void givenServiceThrows_whenGetPopulatedData_thenExceptionPropagates() {
        // given
        UUID taxReturnId = UUID.randomUUID();
        String profile = "bad-profile";

        when(request.getHeader("x-data-import-profile")).thenReturn(profile);
        when(request.getHeader("x-data-import-dob")).thenReturn(null);
        when(mockDataImportService.getPopulatedData(profile, null))
                .thenThrow(new NullPointerException("missing key in map"));

        // when / then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getPopulatedData(taxReturnId))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("missing key in map");
    }

    /**
     * Builds a minimal WrappedPopulatedData instance for test assertions.
     */
    private WrappedPopulatedData buildWrappedPopulatedData() {
        WrappedPopulatedDataNode emptyNode = new WrappedPopulatedDataNode();
        return new WrappedPopulatedData(new WrappedPopulatedData.Data(
                emptyNode, emptyNode, emptyNode, emptyNode, emptyNode, 5000L));
    }
}
