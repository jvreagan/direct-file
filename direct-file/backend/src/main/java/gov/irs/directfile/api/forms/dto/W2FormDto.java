package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record W2FormDto(
        UUID id,
        @NotBlank @Size(max = 100) String employerName,
        @NotBlank @Pattern(regexp = "\\d{2}-\\d{7}", message = "EIN must be in XX-XXXXXXX format") String ein,
        @NotNull @DecimalMin("0") BigDecimal wages,
        @NotNull @DecimalMin("0") BigDecimal federalWithholding,
        @DecimalMin("0") BigDecimal socialSecurityWages,
        @DecimalMin("0") BigDecimal socialSecurityTaxWithheld,
        @DecimalMin("0") BigDecimal medicareWages,
        @DecimalMin("0") BigDecimal medicareTaxWithheld,
        @Size(max = 100) String employerAddress,
        @Pattern(regexp = "[A-Z]{2}") String employerState,
        @DecimalMin("0") BigDecimal stateWages,
        @DecimalMin("0") BigDecimal stateTaxWithheld,
        @DecimalMin("0") BigDecimal localWages,
        @DecimalMin("0") BigDecimal localTaxWithheld,
        @Size(max = 50) String localityName) {}
