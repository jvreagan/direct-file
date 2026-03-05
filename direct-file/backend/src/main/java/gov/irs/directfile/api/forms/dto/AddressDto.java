package gov.irs.directfile.api.forms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank @Size(max = 100) String street,
        @Size(max = 100) String street2,
        @NotBlank @Size(max = 50) String city,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "State must be 2 uppercase letters") String state,
        @NotBlank @Pattern(regexp = "\\d{5}(-\\d{4})?", message = "ZIP must be 5 digits or ZIP+4") String zip) {}
