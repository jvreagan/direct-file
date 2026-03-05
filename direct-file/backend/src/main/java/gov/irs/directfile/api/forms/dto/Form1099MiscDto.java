package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record Form1099MiscDto(
        UUID id,
        @NotBlank @Size(max = 75) String payerName,
        @Pattern(regexp = "\\d{2}-\\d{7}") String payerTin,
        @DecimalMin("0") BigDecimal rents,
        @DecimalMin("0") BigDecimal royalties,
        @DecimalMin("0") BigDecimal otherIncome,
        @DecimalMin("0") BigDecimal federalTaxWithheld) {}
