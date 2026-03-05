package gov.irs.directfile.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import gov.irs.directfile.api.forms.dto.*;
import gov.irs.directfile.api.forms.translation.FormTranslationService;
import gov.irs.directfile.api.loaders.service.FactGraphService;
import gov.irs.directfile.api.pdf.PdfService;
import gov.irs.directfile.api.taxreturn.TaxReturnService;
import gov.irs.directfile.api.taxreturn.dto.Status;
import gov.irs.directfile.api.taxreturn.dto.StatusResponseBody;
import gov.irs.directfile.api.taxreturn.models.TaxReturn;
import gov.irs.directfile.api.user.UserService;
import gov.irs.directfile.api.user.domain.UserInfo;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TaxReturnV2ControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaxReturnService taxReturnService;

    @Mock
    private UserService userService;

    @Mock
    private FactGraphService factGraphService;

    @Mock
    private FormTranslationService formTranslationService;

    @Mock
    private PdfService pdfService;

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_EXTERNAL_ID = UUID.randomUUID();
    private static final UserInfo TEST_USER_INFO =
            new UserInfo(TEST_USER_ID, TEST_EXTERNAL_ID, "test@example.com", "123456789");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        TaxReturnV2Controller controller = new TaxReturnV2Controller(
                taxReturnService, userService, factGraphService, formTranslationService, pdfService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --- Tax Return CRUD ---

    @Test
    void givenValidCreateRequest_whenCreateTaxReturn_thenReturnsCreated() throws Exception {
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(formTranslationService.translateFilerToFacts(any())).thenReturn(new LinkedHashMap<>());
        when(formTranslationService.translateAddressToFacts(any())).thenReturn(new LinkedHashMap<>());

        TaxReturn createdReturn = TaxReturn.testObjectFactory();
        createdReturn.setTaxYear(2024);
        when(taxReturnService.create(anyInt(), anyMap(), any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(createdReturn);

        TaxReturnCreateRequest request = new TaxReturnCreateRequest(
                new FilerDto("John", null, "Doe", null, LocalDate.of(1990, 1, 1), "123456789"),
                null,
                new AddressDto("123 Main St", null, "Springfield", "IL", "62701"),
                FilingStatusDto.SINGLE,
                2024);

        mockMvc.perform(post("/v2/tax-returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdReturn.getId().toString()))
                .andExpect(jsonPath("$.tax_year").value(2024))
                .andExpect(jsonPath("$.filing_status").value("SINGLE"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(taxReturnService)
                .create(
                        eq(2024),
                        anyMap(),
                        eq(TEST_USER_ID),
                        eq("test@example.com"),
                        eq("123456789"),
                        any(),
                        anyInt(),
                        any());
    }

    @Test
    void givenTaxReturnId_whenGetTaxReturn_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        mockMvc.perform(get("/v2/tax-returns/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taxReturn.getId().toString()))
                .andExpect(jsonPath("$.status").exists());

        verify(taxReturnService).findByIdAndUserId(id, TEST_USER_ID);
    }

    @Test
    void givenNonexistentId_whenGetTaxReturn_thenReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v2/tax-returns/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void givenTaxReturnId_whenDeleteTaxReturn_thenReturnsNotImplemented() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        mockMvc.perform(delete("/v2/tax-returns/{id}", id)).andExpect(status().isNotImplemented());
    }

    // --- W-2 ---

    @Test
    void givenValidW2_whenAddW2_thenReturnsCreated() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(formTranslationService.translateW2ToFacts(any(), any())).thenReturn(new LinkedHashMap<>());

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnService.update(any(), anyMap(), isNull(), isNull(), any()))
                .thenReturn(taxReturn);

        W2FormDto dto = new W2FormDto(
                null,
                "Acme Corp",
                "12-3456789",
                new BigDecimal("50000"),
                new BigDecimal("7500"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        mockMvc.perform(post("/v2/tax-returns/{id}/w2s", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.form_type").value("W-2"))
                .andExpect(jsonPath("$.employer_name").value("Acme Corp"));

        verify(taxReturnService).update(eq(taxReturnId), anyMap(), isNull(), isNull(), eq(TEST_USER_ID));
    }

    @Test
    void givenTaxReturnId_whenListW2s_thenReturnsOk() throws Exception {
        UUID taxReturnId = UUID.randomUUID();

        mockMvc.perform(get("/v2/tax-returns/{id}/w2s", taxReturnId)).andExpect(status().isOk());
    }

    @Test
    void givenValidW2_whenUpdateW2_thenReturnsOk() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
        UUID w2Id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(formTranslationService.translateW2ToFacts(any(), any())).thenReturn(new LinkedHashMap<>());

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnService.update(any(), anyMap(), isNull(), isNull(), any()))
                .thenReturn(taxReturn);

        W2FormDto dto = new W2FormDto(
                w2Id,
                "Updated Corp",
                "12-3456789",
                new BigDecimal("60000"),
                new BigDecimal("9000"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        mockMvc.perform(put("/v2/tax-returns/{id}/w2s/{w2Id}", taxReturnId, w2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true));
    }

    @Test
    void givenW2Id_whenDeleteW2_thenReturnsNoContent() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
        UUID w2Id = UUID.randomUUID();

        mockMvc.perform(delete("/v2/tax-returns/{id}/w2s/{w2Id}", taxReturnId, w2Id))
                .andExpect(status().isNoContent());
    }

    // --- 1099 Forms ---

    @Test
    void givenValid1099Int_whenAdd_thenReturnsCreated() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(formTranslationService.translate1099IntToFacts(any(), any())).thenReturn(new LinkedHashMap<>());

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnService.update(any(), anyMap(), isNull(), isNull(), any()))
                .thenReturn(taxReturn);

        Form1099IntDto dto = new Form1099IntDto(
                null, "Big Bank", "12-3456789", new BigDecimal("500"), null, null, null, null, null, true);

        mockMvc.perform(post("/v2/tax-returns/{id}/1099-int", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.form_type").value("1099-INT"));
    }

    @Test
    void givenValid1099Div_whenAdd_thenReturnsCreated() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);
        when(formTranslationService.translate1099DivToFacts(any(), any())).thenReturn(new LinkedHashMap<>());

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(taxReturnId, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));
        when(taxReturnService.update(any(), anyMap(), isNull(), isNull(), any()))
                .thenReturn(taxReturn);

        Form1099DivDto dto =
                new Form1099DivDto(null, "Fund Co", "12-3456789", new BigDecimal("1000"), null, null, null, null, null);

        mockMvc.perform(post("/v2/tax-returns/{id}/1099-div", taxReturnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.form_type").value("1099-DIV"));
    }

    // --- Summary, Validate, Submit, Status ---

    @Test
    void givenTaxReturnId_whenGetSummary_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        mockMvc.perform(get("/v2/tax-returns/{id}/summary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxReturnId").value(id.toString()));
    }

    @Test
    void givenTaxReturnId_whenValidate_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));
        when(factGraphService.factsParseCorrectly(anyMap())).thenReturn(true);
        when(factGraphService.getGraph(anyMap())).thenReturn(null);
        when(factGraphService.hasSubmissionBlockingFacts(any())).thenReturn(false);

        mockMvc.perform(post("/v2/tax-returns/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void givenTaxReturnId_whenSubmit_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        TaxReturn taxReturn = TaxReturn.testObjectFactory();
        taxReturn.setFacts(new LinkedHashMap<>());
        when(taxReturnService.findByIdAndUserId(id, TEST_USER_ID)).thenReturn(Optional.of(taxReturn));

        TaxReturn submittedReturn = TaxReturn.testObjectFactory();
        submittedReturn.setMostRecentSubmitTime(new java.util.Date());
        when(taxReturnService.submit(any(), anyMap(), any(), any(), any(), anyInt(), any()))
                .thenReturn(submittedReturn);

        mockMvc.perform(post("/v2/tax-returns/{id}/submit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(taxReturnService).submit(eq(id), anyMap(), eq(TEST_USER_ID), eq(TEST_USER_INFO), any(), anyInt(), any());
    }

    @Test
    void givenTaxReturnId_whenGetStatus_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getCurrentUserInfo()).thenReturn(TEST_USER_INFO);

        StatusResponseBody statusBody =
                new StatusResponseBody(Status.Pending, "status.pending", List.of(), new java.util.Date());
        when(taxReturnService.getStatus(id, TEST_USER_ID)).thenReturn(statusBody);

        mockMvc.perform(get("/v2/tax-returns/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.tax_return_id").value(id.toString()));
    }
}
