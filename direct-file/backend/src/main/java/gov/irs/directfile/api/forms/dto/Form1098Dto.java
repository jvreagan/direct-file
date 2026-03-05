package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.*;

public record Form1098Dto(
        UUID id,
        @NotBlank @Size(max = 75) String lenderName,
        @Pattern(regexp = "\\d{2}-\\d{7}") String lenderTin,
        @NotNull @DecimalMin("0") BigDecimal mortgageInterest,
        @DecimalMin("0") BigDecimal outstandingPrincipal,
        @DecimalMin("0") BigDecimal mortgageInsurancePremiums,
        @DecimalMin("0") BigDecimal pointsPaid) {}
