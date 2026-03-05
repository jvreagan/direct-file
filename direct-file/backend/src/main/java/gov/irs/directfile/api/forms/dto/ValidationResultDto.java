package gov.irs.directfile.api.forms.dto;

import java.util.List;

public record ValidationResultDto(
        boolean valid, List<String> errors, List<String> warnings, List<String> blockingFacts) {}
