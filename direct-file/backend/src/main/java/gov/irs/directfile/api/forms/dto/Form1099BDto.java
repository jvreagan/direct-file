package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record Form1099BDto(
        UUID id,
        @NotBlank @Size(max = 75) String brokerName,
        @Pattern(regexp = "\\d{2}-\\d{7}") String brokerTin,
        @NotBlank @Size(max = 100) String description,
        LocalDate dateAcquired,
        @NotNull LocalDate dateSold,
        @NotNull BigDecimal proceeds,
        @NotNull BigDecimal costBasis,
        @DecimalMin("0") BigDecimal washSaleLossDisallowed,
        @DecimalMin("0") BigDecimal federalTaxWithheld,
        boolean shortTerm,
        boolean basisReportedToIRS) {}
