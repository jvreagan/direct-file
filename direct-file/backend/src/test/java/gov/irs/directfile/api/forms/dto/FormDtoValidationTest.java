package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    class W2FormDtoTests {
        @Test
        void givenValidW2_whenValidate_thenNoViolations() {
            W2FormDto dto = new W2FormDto(
                    UUID.randomUUID(),
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

            Set<ConstraintViolation<W2FormDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingEmployerName_whenValidate_thenHasViolation() {
            W2FormDto dto = new W2FormDto(
                    null,
                    "",
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

            Set<ConstraintViolation<W2FormDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }

        @Test
        void givenInvalidEinFormat_whenValidate_thenHasViolation() {
            W2FormDto dto = new W2FormDto(
                    null,
                    "Acme Corp",
                    "invalid-ein",
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

            Set<ConstraintViolation<W2FormDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }

        @Test
        void givenNegativeWages_whenValidate_thenHasViolation() {
            W2FormDto dto = new W2FormDto(
                    null,
                    "Acme Corp",
                    "12-3456789",
                    new BigDecimal("-1"),
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

            Set<ConstraintViolation<W2FormDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class FilerDtoTests {
        @Test
        void givenValidFiler_whenValidate_thenNoViolations() {
            FilerDto dto = new FilerDto("John", null, "Doe", null, LocalDate.of(1990, 1, 1), "123456789");

            Set<ConstraintViolation<FilerDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenInvalidSsnFormat_whenValidate_thenHasViolation() {
            FilerDto dto = new FilerDto("John", null, "Doe", null, LocalDate.of(1990, 1, 1), "12345");

            Set<ConstraintViolation<FilerDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }

        @Test
        void givenMissingFirstName_whenValidate_thenHasViolation() {
            FilerDto dto = new FilerDto("", null, "Doe", null, LocalDate.of(1990, 1, 1), "123456789");

            Set<ConstraintViolation<FilerDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class AddressDtoTests {
        @Test
        void givenValidAddress_whenValidate_thenNoViolations() {
            AddressDto dto = new AddressDto("123 Main St", null, "Springfield", "IL", "62701");

            Set<ConstraintViolation<AddressDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenValidZipPlus4_whenValidate_thenNoViolations() {
            AddressDto dto = new AddressDto("123 Main St", null, "Springfield", "IL", "62701-1234");

            Set<ConstraintViolation<AddressDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenInvalidStateFormat_whenValidate_thenHasViolation() {
            AddressDto dto = new AddressDto("123 Main St", null, "Springfield", "Illinois", "62701");

            Set<ConstraintViolation<AddressDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }

        @Test
        void givenInvalidZipFormat_whenValidate_thenHasViolation() {
            AddressDto dto = new AddressDto("123 Main St", null, "Springfield", "IL", "abc");

            Set<ConstraintViolation<AddressDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class Form1099IntDtoTests {
        @Test
        void givenValid1099Int_whenValidate_thenNoViolations() {
            Form1099IntDto dto = new Form1099IntDto(
                    null, "Big Bank", "12-3456789", new BigDecimal("500"), null, null, null, null, null, true);

            Set<ConstraintViolation<Form1099IntDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingPayerName_whenValidate_thenHasViolation() {
            Form1099IntDto dto =
                    new Form1099IntDto(null, "", null, new BigDecimal("500"), null, null, null, null, null, true);

            Set<ConstraintViolation<Form1099IntDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class Form1099DivDtoTests {
        @Test
        void givenValid1099Div_whenValidate_thenNoViolations() {
            Form1099DivDto dto = new Form1099DivDto(
                    null, "Fund Co", "12-3456789", new BigDecimal("1000"), null, null, null, null, null);

            Set<ConstraintViolation<Form1099DivDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingOrdinaryDividends_whenValidate_thenHasViolation() {
            Form1099DivDto dto = new Form1099DivDto(null, "Fund Co", "12-3456789", null, null, null, null, null, null);

            Set<ConstraintViolation<Form1099DivDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class Form1099BDtoTests {
        @Test
        void givenValid1099B_whenValidate_thenNoViolations() {
            Form1099BDto dto = new Form1099BDto(
                    null,
                    "Brokerage",
                    null,
                    "100 AAPL",
                    null,
                    LocalDate.of(2024, 6, 15),
                    new BigDecimal("15000"),
                    new BigDecimal("10000"),
                    null,
                    null,
                    false,
                    true);

            Set<ConstraintViolation<Form1099BDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingBrokerName_whenValidate_thenHasViolation() {
            Form1099BDto dto = new Form1099BDto(
                    null,
                    "",
                    null,
                    "100 AAPL",
                    null,
                    LocalDate.of(2024, 6, 15),
                    new BigDecimal("15000"),
                    new BigDecimal("10000"),
                    null,
                    null,
                    false,
                    true);

            Set<ConstraintViolation<Form1099BDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class Form1099RDtoTests {
        @Test
        void givenValid1099R_whenValidate_thenNoViolations() {
            Form1099RDto dto = new Form1099RDto(
                    null, "Pension Fund", "12-3456789", new BigDecimal("25000"), null, null, "7", false);

            Set<ConstraintViolation<Form1099RDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    class Form1099GDtoTests {
        @Test
        void givenValid1099G_whenValidate_thenNoViolations() {
            Form1099GDto dto =
                    new Form1099GDto(null, "State Agency", "12-3456789", new BigDecimal("5000"), null, null, "CA");

            Set<ConstraintViolation<Form1099GDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    class Form1099MiscDtoTests {
        @Test
        void givenValid1099Misc_whenValidate_thenNoViolations() {
            Form1099MiscDto dto = new Form1099MiscDto(
                    null, "Contractor Inc", "12-3456789", new BigDecimal("1000"), null, new BigDecimal("500"), null);

            Set<ConstraintViolation<Form1099MiscDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    class Form1098DtoTests {
        @Test
        void givenValid1098_whenValidate_thenNoViolations() {
            Form1098Dto dto =
                    new Form1098Dto(null, "Mortgage Co", "12-3456789", new BigDecimal("12000"), null, null, null);

            Set<ConstraintViolation<Form1098Dto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingLenderName_whenValidate_thenHasViolation() {
            Form1098Dto dto = new Form1098Dto(null, "", null, new BigDecimal("12000"), null, null, null);

            Set<ConstraintViolation<Form1098Dto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
        }
    }

    @Nested
    class TaxReturnCreateRequestTests {
        @Test
        void givenValidRequest_whenValidate_thenNoViolations() {
            FilerDto filer = new FilerDto("John", null, "Doe", null, LocalDate.of(1990, 1, 1), "123456789");
            AddressDto address = new AddressDto("123 Main St", null, "Springfield", "IL", "62701");
            TaxReturnCreateRequest req = new TaxReturnCreateRequest(filer, null, address, FilingStatusDto.SINGLE, 2024);

            Set<ConstraintViolation<TaxReturnCreateRequest>> violations = validator.validate(req);
            assertTrue(violations.isEmpty());
        }

        @Test
        void givenMissingFiler_whenValidate_thenHasViolation() {
            TaxReturnCreateRequest req = new TaxReturnCreateRequest(null, null, null, FilingStatusDto.SINGLE, 2024);

            Set<ConstraintViolation<TaxReturnCreateRequest>> violations = validator.validate(req);
            assertFalse(violations.isEmpty());
        }

        @Test
        void givenZeroTaxYear_whenConstruct_thenDefaultsTo2024() {
            FilerDto filer = new FilerDto("John", null, "Doe", null, LocalDate.of(1990, 1, 1), "123456789");
            TaxReturnCreateRequest req = new TaxReturnCreateRequest(filer, null, null, FilingStatusDto.SINGLE, 0);

            assertEquals(2024, req.taxYear());
        }
    }
}
