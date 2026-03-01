package gov.irs.directfile.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
import gov.irs.directfile.api.user.UserService;

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
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tax_year").value(2024))
                .andExpect(jsonPath("$.filing_status").value("SINGLE"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void givenTaxReturnId_whenGetTaxReturn_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/v2/tax-returns/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void givenTaxReturnId_whenDeleteTaxReturn_thenReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v2/tax-returns/{id}", id)).andExpect(status().isNoContent());
    }

    // --- W-2 ---

    @Test
    void givenValidW2_whenAddW2_thenReturnsCreated() throws Exception {
        UUID taxReturnId = UUID.randomUUID();
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

        mockMvc.perform(get("/v2/tax-returns/{id}/summary", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxReturnId").value(id.toString()));
    }

    @Test
    void givenTaxReturnId_whenValidate_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/v2/tax-returns/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void givenTaxReturnId_whenSubmit_thenReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/v2/tax-returns/{id}/submit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}
