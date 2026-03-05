package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record Form1099IntDto(
        UUID id,
        @NotBlank @Size(max = 75) String payerName,
        @Pattern(regexp = "\\d{2}-\\d{7}") String payerTin,
        @NotNull @DecimalMin("0") BigDecimal interestIncome,
        @DecimalMin("0") BigDecimal earlyWithdrawalPenalty,
        @DecimalMin("0") BigDecimal interestOnGovernmentBonds,
        @DecimalMin("0") BigDecimal federalTaxWithheld,
        @DecimalMin("0") BigDecimal foreignTaxPaid,
        @DecimalMin("0") BigDecimal taxExemptInterest,
        boolean has1099) {}
