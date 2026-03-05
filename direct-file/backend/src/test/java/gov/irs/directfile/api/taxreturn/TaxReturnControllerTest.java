package gov.irs.directfile.api.taxreturn;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import gov.irs.directfile.api.dataimport.DataImportService;
import gov.irs.directfile.api.dataimport.model.WrappedPopulatedData;
import gov.irs.directfile.api.dataimport.model.WrappedPopulatedDataNode;
import gov.irs.directfile.api.errors.InvalidOperationException;
import gov.irs.directfile.api.errors.NonexistentDataException;
import gov.irs.directfile.api.pdf.PdfCreationException;
import gov.irs.directfile.api.pdf.PdfService;
import gov.irs.directfile.api.taxreturn.dto.Status;
import gov.irs.directfile.api.taxreturn.dto.StatusResponseBody;
import gov.irs.directfile.api.taxreturn.models.TaxReturn;
import gov.irs.directfile.api.user.UserService;
import gov.irs.directfile.api.user.domain.UserInfo;
import gov.irs.directfile.models.FactTypeWithItem;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc tests for {@link TaxReturnController}.
 *
 * <p>Uses {@code @ExtendWith(MockitoExtension.class)} with
 * {@code MockMvcBuilders.standaloneSetup} -- no Spring Boot context is loaded.
 * The {@code ${direct-file.api-version}} property placeholder in the interface's
 * {@code @RequestMapping} is resolved to {@code "v1"} via
 * {@code addPlaceholderValue}.
 */
@ExtendWith(MockitoExtension.class)
class TaxReturnControllerTest {

    private static final String API_VERSION = "v1";
    private static final String BASE_URL = "/" + API_VERSION + "/taxreturns";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @Mock
    private TaxReturnService taxReturnService;

    @Mock
    private UserService userService;

    @Mock
    private PdfService pdfService;

    @Mock
    private EncryptionCacheWarmingService cacheWarmingService;

    @Mock
    private DataImportService dataImportService;

    private UserInfo userInfo;
    private UUID userId;
    private UUID externalId;
    private UUID taxReturnId;
    private TaxReturn taxReturn;

    @BeforeEach
    void setUp() {
        TaxReturnController controller = new TaxReturnController(
                taxReturnService, userService, pdfService, cacheWarmingService, dataImportService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("direct-file.api-version", API_VERSION)
                .build();

        userId = UUID.randomUUID();
        externalId = UUID.randomUUID();
        userInfo = new UserInfo(userId, externalId, "test@example.com", "123456789");
        taxReturnId = UUID.randomUUID();
        taxReturn = TaxReturn.testObjectFactory();
    }

    // -----------------------------------------------------------------------
    // GET /v1/taxreturns  (getAllByUserId)
    // -----------------------------------------------------------------------

    @Test
    void givenAuthenticatedUser_whenGetAllByUserId_thenReturnsOkWithList() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByUserId(userId)).thenReturn(List.of(taxReturn));
        when(taxReturnService.isTaxReturnEditable(taxReturn.getId())).thenReturn(true);

        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taxReturn.getId().toString()));
    }

    @Test
    void givenUserWithNoTaxReturns_whenGetAllByUserId_thenReturnsEmptyList() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByUserId(userId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -----------------------------------------------------------------------
    // GET /v1/taxreturns/{id}  (getById)
    // -----------------------------------------------------------------------

    @Test
    void givenExistingTaxReturn_whenGetById_thenReturnsOk() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.of(taxReturn));
        when(taxReturnService.isTaxReturnEditable(taxReturn.getId())).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/{id}", taxReturnId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxReturn.getId().toString()))
                .andExpect(jsonPath("$.taxYear").value(taxReturn.getTaxYear()));
    }

    @Test
    void givenNonexistentTaxReturn_whenGetById_thenReturns404() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/{id}", taxReturnId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /v1/taxreturns  (create)
    // -----------------------------------------------------------------------

    @Test
    void givenValidCreateRequest_whenCreate_thenReturns201WithLocationHeader() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);

        Map<String, FactTypeWithItem> facts = new HashMap<>();
        when(taxReturnService.create(
                eq(2024), eq(facts), eq(userId), eq(userInfo.email()), eq(userInfo.tin()),
                anyString(), anyInt(), any()))
                .thenReturn(taxReturn);
        when(taxReturnService.isTaxReturnEditable(taxReturn.getId())).thenReturn(true);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "taxYear", 2024,
                "facts", facts));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(taxReturn.getId().toString()));
    }

    @Test
    void givenDuplicateTaxYear_whenCreate_thenReturns409() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.create(
                anyInt(), any(), any(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new InvalidOperationException("Already exists"));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "taxYear", 2024,
                "facts", new HashMap<>()));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void givenNonexistentData_whenCreate_thenReturns404() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.create(
                anyInt(), any(), any(), anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new NonexistentDataException("Not found"));

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "taxYear", 2024,
                "facts", new HashMap<>()));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // POST /v1/taxreturns/{id}  (update)
    // -----------------------------------------------------------------------

    @Test
    void givenValidUpdateRequest_whenUpdate_thenReturns204() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);

        Map<String, FactTypeWithItem> facts = new HashMap<>();
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "facts", facts));

        mockMvc.perform(post(BASE_URL + "/{id}", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("Location"));

        verify(taxReturnService).update(eq(taxReturnId), eq(facts), isNull(), isNull(), eq(userId));
    }

    @Test
    void givenAlreadySubmittedReturn_whenUpdate_thenReturns409() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        doThrow(new InvalidOperationException("Already submitted"))
                .when(taxReturnService).update(any(), any(), any(), any(), any());

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "facts", new HashMap<>()));

        mockMvc.perform(post(BASE_URL + "/{id}", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    // -----------------------------------------------------------------------
    // POST /v1/taxreturns/{id}/submit  (submit)
    // -----------------------------------------------------------------------

    @Test
    void givenValidSubmitRequest_whenSubmit_thenReturns202() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        TaxReturn submittedReturn = TaxReturn.testObjectFactory();
        submittedReturn.setMostRecentSubmitTime(new Date());
        when(taxReturnService.submit(
                eq(taxReturnId), any(), eq(userId), eq(userInfo), anyString(), anyInt(), any()))
                .thenReturn(submittedReturn);

        Map<String, FactTypeWithItem> facts = new HashMap<>();
        String requestBody = objectMapper.writeValueAsString(Map.of("facts", facts));

        mockMvc.perform(post(BASE_URL + "/{id}/submit", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("was dispatched to the electronic filing queue")));
    }

    // -----------------------------------------------------------------------
    // POST /v1/taxreturns/{id}/sign  (sign)
    // -----------------------------------------------------------------------

    @Test
    void givenValidSignRequest_whenSign_thenReturns202() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        TaxReturn signedReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.submit(
                eq(taxReturnId), any(), eq(userId), eq(userInfo), anyString(), anyInt(), any()))
                .thenReturn(signedReturn);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "facts", new HashMap<>(),
                "intentStatement", "I agree to the terms."));

        mockMvc.perform(post(BASE_URL + "/{id}/sign", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(content().string(containsString("Signed request")));
    }

    // -----------------------------------------------------------------------
    // GET /v1/taxreturns/{id}/status  (status)
    // -----------------------------------------------------------------------

    @Test
    void givenSubmittedReturn_whenGetStatus_thenReturnsStatusBody() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        StatusResponseBody statusResponseBody = new StatusResponseBody(
                Status.Accepted, "status.accepted", Collections.emptyList(), new Date());
        when(taxReturnService.getStatus(taxReturnId, userId)).thenReturn(statusResponseBody);

        mockMvc.perform(get(BASE_URL + "/{id}/status", taxReturnId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Accepted"))
                .andExpect(jsonPath("$.translationKey").value("status.accepted"));
    }

    // -----------------------------------------------------------------------
    // POST /v1/taxreturns/{id}/pdf/{languageCode}  (pdf)
    // -----------------------------------------------------------------------

    @Test
    void givenExistingTaxReturn_whenGetPdf_thenReturnsPdfStream() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.of(taxReturn));

        byte[] pdfBytes = "fake-pdf-content".getBytes(StandardCharsets.UTF_8);
        InputStream pdfStream = new ByteArrayInputStream(pdfBytes);
        when(pdfService.getTaxReturn(eq("en"), eq(taxReturn), eq(false))).thenReturn(pdfStream);

        mockMvc.perform(post(BASE_URL + "/{id}/pdf/{languageCode}", taxReturnId, "en")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("taxreturn")));
    }

    @Test
    void givenNonexistentTaxReturn_whenGetPdf_thenReturns404() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE_URL + "/{id}/pdf/{languageCode}", taxReturnId, "en")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenPdfCreationFailure_whenGetPdf_thenReturns500() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.of(taxReturn));
        when(pdfService.getTaxReturn(anyString(), eq(taxReturn), eq(false)))
                .thenThrow(new PdfCreationException("PDF generation failed", new RuntimeException()));

        mockMvc.perform(post(BASE_URL + "/{id}/pdf/{languageCode}", taxReturnId, "en")
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isInternalServerError());
    }

    // -----------------------------------------------------------------------
    // GET /v1/taxreturns/{id}/populate  (getPopulatedData)
    // -----------------------------------------------------------------------

    @Test
    void givenExistingTaxReturn_whenGetPopulatedData_thenReturnsData() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.of(taxReturn));

        WrappedPopulatedData.Data data = new WrappedPopulatedData.Data(
                new WrappedPopulatedDataNode(),
                new WrappedPopulatedDataNode(),
                new WrappedPopulatedDataNode(),
                new WrappedPopulatedDataNode(),
                new WrappedPopulatedDataNode(),
                1000L);
        WrappedPopulatedData wrappedData = new WrappedPopulatedData(data);
        when(dataImportService.getPopulatedData(taxReturnId, userId, taxReturn.getCreatedAt()))
                .thenReturn(wrappedData);

        mockMvc.perform(get(BASE_URL + "/{id}/populate", taxReturnId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void givenNonexistentTaxReturn_whenGetPopulatedData_thenReturns404() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(userInfo);
        when(taxReturnService.findByIdAndUserId(taxReturnId, userId)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_URL + "/{id}/populate", taxReturnId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
