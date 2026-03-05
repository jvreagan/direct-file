package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record Form1099DivDto(
        UUID id,
        @NotBlank @Size(max = 75) String payerName,
        @Pattern(regexp = "\\d{2}-\\d{7}") String payerTin,
        @NotNull @DecimalMin("0") BigDecimal ordinaryDividends,
        @DecimalMin("0") BigDecimal qualifiedDividends,
        @DecimalMin("0") BigDecimal totalCapitalGainDistributions,
        @DecimalMin("0") BigDecimal section199ADividends,
        @DecimalMin("0") BigDecimal federalTaxWithheld,
        @DecimalMin("0") BigDecimal exemptInterestDividends) {}
