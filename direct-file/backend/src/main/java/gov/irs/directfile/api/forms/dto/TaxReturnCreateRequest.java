package gov.irs.directfile.api.forms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record TaxReturnCreateRequest(
        @NotNull @Valid FilerDto filer,
        @Valid FilerDto spouse,
        @Valid AddressDto address,
        @NotNull FilingStatusDto filingStatus,
        int taxYear) {
    public TaxReturnCreateRequest {
        if (taxYear == 0) taxYear = 2024;
    }
}
