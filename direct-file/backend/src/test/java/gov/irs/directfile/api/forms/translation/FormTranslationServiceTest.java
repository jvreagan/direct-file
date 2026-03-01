package gov.irs.directfile.api.forms.translation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gov.irs.directfile.api.forms.dto.*;

import static org.junit.jupiter.api.Assertions.*;

class FormTranslationServiceTest {
    private FormTranslationService service;

    @BeforeEach
    void setUp() {
        service = new FormTranslationService();
    }

    @Test
    void givenW2Dto_whenTranslate_thenAllFieldsMapped() {
        UUID itemId = UUID.randomUUID();
        W2FormDto dto = new W2FormDto(
                itemId,
                "Acme Corp",
                "12-3456789",
                new BigDecimal("50000.00"),
                new BigDecimal("7500.00"),
                new BigDecimal("48000.00"),
                new BigDecimal("2976.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("725.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        Map<String, Object> facts = service.translateW2ToFacts(dto, itemId);

        String id = itemId.toString();
        assertEquals("Acme Corp", facts.get(String.format(FactPathConstants.W2_EMPLOYER_NAME, id)));
        assertEquals("12-3456789", facts.get(String.format(FactPathConstants.W2_EIN, id)));
        assertEquals("50000.00", facts.get(String.format(FactPathConstants.W2_WAGES, id)));
        assertEquals("7500.00", facts.get(String.format(FactPathConstants.W2_FED_WITHHOLDING, id)));
        assertEquals("48000.00", facts.get(String.format(FactPathConstants.W2_SS_WAGES, id)));
        assertEquals("2976.00", facts.get(String.format(FactPathConstants.W2_SS_TAX, id)));
        assertEquals("50000.00", facts.get(String.format(FactPathConstants.W2_MEDICARE_WAGES, id)));
        assertEquals("725.00", facts.get(String.format(FactPathConstants.W2_MEDICARE_TAX, id)));
    }

    @Test
    void givenW2DtoWithNullOptionals_whenTranslate_thenNullFieldsSkipped() {
        UUID itemId = UUID.randomUUID();
        W2FormDto dto = new W2FormDto(
                itemId,
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

        Map<String, Object> facts = service.translateW2ToFacts(dto, itemId);

        String id = itemId.toString();
        assertNotNull(facts.get(String.format(FactPathConstants.W2_WAGES, id)));
        assertNull(facts.get(String.format(FactPathConstants.W2_SS_WAGES, id)));
        assertNull(facts.get(String.format(FactPathConstants.W2_SS_TAX, id)));
        assertNull(facts.get(String.format(FactPathConstants.W2_MEDICARE_WAGES, id)));
        assertNull(facts.get(String.format(FactPathConstants.W2_MEDICARE_TAX, id)));
    }

    @Test
    void given1099IntDto_whenTranslate_thenAllFieldsMapped() {
        UUID itemId = UUID.randomUUID();
        Form1099IntDto dto = new Form1099IntDto(
                itemId,
                "Big Bank",
                "98-7654321",
                new BigDecimal("1200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("500.00"),
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("75.00"),
                true);

        Map<String, Object> facts = service.translate1099IntToFacts(dto, itemId);

        String id = itemId.toString();
        assertEquals("Big Bank", facts.get(String.format(FactPathConstants.INT_PAYER, id)));
        assertEquals("98-7654321", facts.get(String.format(FactPathConstants.INT_PAYER_TIN, id)));
        assertEquals("1200.00", facts.get(String.format(FactPathConstants.INT_1099_AMOUNT, id)));
        assertEquals("100.00", facts.get(String.format(FactPathConstants.INT_TAX_WITHHELD, id)));
        assertEquals("25.00", facts.get(String.format(FactPathConstants.INT_FOREIGN_TAX, id)));
        assertEquals("75.00", facts.get(String.format(FactPathConstants.INT_TAX_EXEMPT, id)));
        assertEquals("50.00", facts.get(String.format(FactPathConstants.INT_EARLY_WITHDRAWAL, id)));
        assertEquals("500.00", facts.get(String.format(FactPathConstants.INT_GOV_BONDS, id)));
        assertEquals(true, facts.get(String.format(FactPathConstants.INT_HAS_1099, id)));
    }

    @Test
    void given1099DivDto_whenTranslate_thenAllFieldsMapped() {
        UUID itemId = UUID.randomUUID();
        Form1099DivDto dto = new Form1099DivDto(
                itemId,
                "Dividend Inc",
                "12-3456789",
                new BigDecimal("5000.00"),
                new BigDecimal("3000.00"),
                null,
                null,
                new BigDecimal("500.00"),
                null);

        Map<String, Object> facts = service.translate1099DivToFacts(dto, itemId);

        String id = itemId.toString();
        assertEquals("Dividend Inc", facts.get(String.format(FactPathConstants.DIV_PAYER_NAME, id)));
        assertEquals("12-3456789", facts.get(String.format(FactPathConstants.DIV_PAYER_TIN, id)));
        assertEquals("5000.00", facts.get(String.format(FactPathConstants.DIV_ORDINARY, id)));
        assertEquals("3000.00", facts.get(String.format(FactPathConstants.DIV_QUALIFIED, id)));
        assertEquals("500.00", facts.get(String.format(FactPathConstants.DIV_FED_WITHHELD, id)));
    }

    @Test
    void given1099BDto_whenTranslate_thenAllFieldsMapped() {
        UUID itemId = UUID.randomUUID();
        LocalDate dateSold = LocalDate.of(2024, 6, 15);
        Form1099BDto dto = new Form1099BDto(
                itemId,
                "Brokerage LLC",
                "11-2233445",
                "100 shares AAPL",
                LocalDate.of(2020, 1, 10),
                dateSold,
                new BigDecimal("15000.00"),
                new BigDecimal("10000.00"),
                null,
                null,
                false,
                true);

        Map<String, Object> facts = service.translate1099BToFacts(dto, itemId);

        String id = itemId.toString();
        assertEquals("Brokerage LLC", facts.get(String.format(FactPathConstants.B_BROKER_NAME, id)));
        assertEquals("100 shares AAPL", facts.get(String.format(FactPathConstants.B_DESCRIPTION, id)));
        assertEquals("2024-06-15", facts.get(String.format(FactPathConstants.B_DATE_SOLD, id)));
        assertEquals("15000.00", facts.get(String.format(FactPathConstants.B_PROCEEDS, id)));
        assertEquals("10000.00", facts.get(String.format(FactPathConstants.B_COST_BASIS, id)));
    }

    @Test
    void givenFilerDto_whenTranslate_thenAllFieldsMapped() {
        FilerDto dto = new FilerDto("John", "M", "Doe", null, LocalDate.of(1990, 5, 15), "123456789");

        Map<String, Object> facts = service.translateFilerToFacts(dto);

        assertEquals("John", facts.get(FactPathConstants.PRIMARY_FILER_FIRST_NAME));
        assertEquals("Doe", facts.get(FactPathConstants.PRIMARY_FILER_LAST_NAME));
        assertEquals("1990-05-15", facts.get(FactPathConstants.PRIMARY_FILER_DOB));
        assertEquals("123456789", facts.get(FactPathConstants.PRIMARY_FILER_TIN));
    }

    @Test
    void givenAddressDto_whenTranslate_thenAllFieldsMapped() {
        AddressDto dto = new AddressDto("123 Main St", "Apt 4B", "Springfield", "IL", "62701");

        Map<String, Object> facts = service.translateAddressToFacts(dto);

        assertEquals("123 Main St", facts.get(FactPathConstants.ADDRESS_STREET));
        assertEquals("Springfield", facts.get(FactPathConstants.ADDRESS_CITY));
        assertEquals("IL", facts.get(FactPathConstants.ADDRESS_STATE));
        assertEquals("62701", facts.get(FactPathConstants.ADDRESS_ZIP));
    }

    @Test
    void givenBigDecimalValues_whenTranslate_thenConvertedToString() {
        UUID itemId = UUID.randomUUID();
        W2FormDto dto = new W2FormDto(
                itemId,
                "Corp",
                "12-3456789",
                new BigDecimal("123.45"),
                new BigDecimal("67.89"),
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

        Map<String, Object> facts = service.translateW2ToFacts(dto, itemId);

        String id = itemId.toString();
        assertInstanceOf(String.class, facts.get(String.format(FactPathConstants.W2_WAGES, id)));
        assertEquals("123.45", facts.get(String.format(FactPathConstants.W2_WAGES, id)));
    }
}
