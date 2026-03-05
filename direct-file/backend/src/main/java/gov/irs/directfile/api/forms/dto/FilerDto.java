package gov.irs.directfile.api.forms.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FilerDto(
        @NotBlank @Size(max = 50) String firstName,
        @Size(max = 50) String middleName,
        @NotBlank @Size(max = 50) String lastName,
        @Size(max = 10) String suffix,
        @NotNull LocalDate dateOfBirth,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "SSN must be 9 digits") String ssn) {}
