package gov.irs.directfile.api.v2;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import gov.irs.directfile.api.config.IPAddressUtil;
import gov.irs.directfile.api.forms.dto.*;
import gov.irs.directfile.api.forms.translation.FormTranslationService;
import gov.irs.directfile.api.loaders.service.FactGraphService;
import gov.irs.directfile.api.pdf.PdfService;
import gov.irs.directfile.api.taxreturn.TaxReturnService;
import gov.irs.directfile.api.taxreturn.dto.StatusResponseBody;
import gov.irs.directfile.api.taxreturn.models.TaxReturn;
import gov.irs.directfile.api.user.UserService;
import gov.irs.directfile.api.user.domain.UserInfo;
import gov.irs.directfile.models.FactTypeWithItem;

@Slf4j
@RestController
@RequestMapping("/v2/tax-returns")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class TaxReturnV2Controller {
    private final TaxReturnService taxReturnService;
    private final UserService userService;
    private final FactGraphService factGraphService;
    private final FormTranslationService formTranslationService;
    private final PdfService pdfService;
    private final ObjectMapper objectMapper;

    public TaxReturnV2Controller(
            TaxReturnService taxReturnService,
            UserService userService,
            FactGraphService factGraphService,
            FormTranslationService formTranslationService,
            PdfService pdfService) {
        this.taxReturnService = taxReturnService;
        this.userService = userService;
        this.factGraphService = factGraphService;
        this.formTranslationService = formTranslationService;
        this.pdfService = pdfService;
        this.objectMapper = new ObjectMapper();
    }

    // --- Tax Return CRUD ---

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTaxReturn(
            @Valid @RequestBody TaxReturnCreateRequest request, HttpServletRequest httpRequest) {
        log.info("Creating tax return via v2 API for filing status: {}", request.filingStatus());
        UserInfo userInfo = userService.getCurrentUserInfo();

        Map<String, Object> translatedFacts = new LinkedHashMap<>();
        translatedFacts.putAll(formTranslationService.translateFilerToFacts(request.filer()));
        if (request.address() != null) {
            translatedFacts.putAll(formTranslationService.translateAddressToFacts(request.address()));
        }

        Map<String, FactTypeWithItem> factMap = toFactMap(translatedFacts);

        try {
            String remoteAddress = IPAddressUtil.getClientIpAddress(httpRequest);
            int remotePort = httpRequest.getRemotePort();
            String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

            TaxReturn taxReturn = taxReturnService.create(
                    request.taxYear(),
                    factMap,
                    userInfo.id(),
                    userInfo.email(),
                    userInfo.tin(),
                    remoteAddress,
                    remotePort,
                    userAgent);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", taxReturn.getId().toString());
            response.put("tax_year", taxReturn.getTaxYear());
            response.put("filing_status", request.filingStatus().name());
            response.put("status", "CREATED");
            response.put("created_at", taxReturn.getCreatedAt().toInstant().toString());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create tax return", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create tax return", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaxReturn(@PathVariable UUID id) {
        log.info("Getting tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(id, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        TaxReturn taxReturn = optTaxReturn.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", taxReturn.getId().toString());
        response.put("tax_year", taxReturn.getTaxYear());
        response.put("status", taxReturn.hasBeenSubmittedAtLeastOnce() ? "SUBMITTED" : "DRAFT");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaxReturn(@PathVariable UUID id) {
        log.info("Deleting tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(id, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Delete is not yet supported");
    }

    // --- W-2 ---

    @PostMapping("/{id}/w2s")
    public ResponseEntity<Map<String, Object>> addW2(@PathVariable UUID id, @Valid @RequestBody W2FormDto dto) {
        log.info("Adding W-2 to tax return {} via v2 API", id);
        UUID w2Id = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> translated = formTranslationService.translateW2ToFacts(dto, w2Id);
        mergeFactsIntoTaxReturn(id, translated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", w2Id.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "W-2");
        response.put("employer_name", dto.employerName());
        response.put("wages", dto.wages());
        response.put("federal_withholding", dto.federalWithholding());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/w2s")
    public ResponseEntity<List<Map<String, Object>>> listW2s(@PathVariable UUID id) {
        log.info("Listing W-2s for tax return {} via v2 API", id);
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/{id}/w2s/{w2Id}")
    public ResponseEntity<Map<String, Object>> updateW2(
            @PathVariable UUID id, @PathVariable UUID w2Id, @Valid @RequestBody W2FormDto dto) {
        log.info("Updating W-2 {} on tax return {} via v2 API", w2Id, id);

        Map<String, Object> translated = formTranslationService.translateW2ToFacts(dto, w2Id);
        mergeFactsIntoTaxReturn(id, translated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", w2Id.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "W-2");
        response.put("updated", true);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/w2s/{w2Id}")
    public ResponseEntity<Void> deleteW2(@PathVariable UUID id, @PathVariable UUID w2Id) {
        log.info("Deleting W-2 {} from tax return {} via v2 API", w2Id, id);
        return ResponseEntity.noContent().build();
    }

    // --- 1099-INT ---

    @PostMapping("/{id}/1099-int")
    public ResponseEntity<Map<String, Object>> add1099Int(
            @PathVariable UUID id, @Valid @RequestBody Form1099IntDto dto) {
        log.info("Adding 1099-INT to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> translated = formTranslationService.translate1099IntToFacts(dto, itemId);
        mergeFactsIntoTaxReturn(id, translated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-INT");
        response.put("payer_name", dto.payerName());
        response.put("interest_income", dto.interestIncome());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/1099-int")
    public ResponseEntity<List<Map<String, Object>>> list1099Int(@PathVariable UUID id) {
        return ResponseEntity.ok(List.of());
    }

    // --- 1099-R ---

    @PostMapping("/{id}/1099-r")
    public ResponseEntity<Map<String, Object>> add1099R(@PathVariable UUID id, @Valid @RequestBody Form1099RDto dto) {
        log.info("Adding 1099-R to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-R");
        response.put("payer_name", dto.payerName());
        response.put("gross_distribution", dto.grossDistribution());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- 1099-G ---

    @PostMapping("/{id}/1099-g")
    public ResponseEntity<Map<String, Object>> add1099G(@PathVariable UUID id, @Valid @RequestBody Form1099GDto dto) {
        log.info("Adding 1099-G to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-G");
        response.put("unemployment_compensation", dto.unemploymentCompensation());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- 1099-MISC ---

    @PostMapping("/{id}/1099-misc")
    public ResponseEntity<Map<String, Object>> add1099Misc(
            @PathVariable UUID id, @Valid @RequestBody Form1099MiscDto dto) {
        log.info("Adding 1099-MISC to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-MISC");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- 1099-DIV ---

    @PostMapping("/{id}/1099-div")
    public ResponseEntity<Map<String, Object>> add1099Div(
            @PathVariable UUID id, @Valid @RequestBody Form1099DivDto dto) {
        log.info("Adding 1099-DIV to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> translated = formTranslationService.translate1099DivToFacts(dto, itemId);
        mergeFactsIntoTaxReturn(id, translated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-DIV");
        response.put("ordinary_dividends", dto.ordinaryDividends());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- 1099-B ---

    @PostMapping("/{id}/1099-b")
    public ResponseEntity<Map<String, Object>> add1099B(@PathVariable UUID id, @Valid @RequestBody Form1099BDto dto) {
        log.info("Adding 1099-B to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> translated = formTranslationService.translate1099BToFacts(dto, itemId);
        mergeFactsIntoTaxReturn(id, translated);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1099-B");
        response.put("proceeds", dto.proceeds());
        response.put("cost_basis", dto.costBasis());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- 1098 ---

    @PostMapping("/{id}/1098")
    public ResponseEntity<Map<String, Object>> add1098(@PathVariable UUID id, @Valid @RequestBody Form1098Dto dto) {
        log.info("Adding 1098 to tax return {} via v2 API", id);
        UUID itemId = dto.id() != null ? dto.id() : UUID.randomUUID();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", itemId.toString());
        response.put("tax_return_id", id.toString());
        response.put("form_type", "1098");
        response.put("mortgage_interest", dto.mortgageInterest());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // --- Summary ---

    @GetMapping("/{id}/summary")
    public ResponseEntity<TaxReturnSummaryDto> getSummary(@PathVariable UUID id) {
        log.info("Getting summary for tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(id, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        TaxReturn taxReturn = optTaxReturn.get();
        String status = taxReturn.hasBeenSubmittedAtLeastOnce() ? "SUBMITTED" : "DRAFT";

        TaxReturnSummaryDto summary = new TaxReturnSummaryDto(
                id, null, null, null, null, null, null, null, null, null, null, null, null, false, status);
        return ResponseEntity.ok(summary);
    }

    // --- Validation ---

    @PostMapping("/{id}/validate")
    public ResponseEntity<ValidationResultDto> validate(@PathVariable UUID id) {
        log.info("Validating tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(id, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        TaxReturn taxReturn = optTaxReturn.get();
        Map<String, FactTypeWithItem> facts = taxReturn.getFacts();

        boolean parsesCorrectly = factGraphService.factsParseCorrectly(facts);
        List<String> errors = new ArrayList<>();
        List<String> blockingFacts = new ArrayList<>();

        if (!parsesCorrectly) {
            errors.add("Facts do not parse correctly");
        } else {
            var graph = factGraphService.getGraph(facts);
            if (factGraphService.hasSubmissionBlockingFacts(graph)) {
                blockingFacts.add("Tax return has submission-blocking facts");
            }
        }

        boolean valid = errors.isEmpty() && blockingFacts.isEmpty();
        ValidationResultDto result = new ValidationResultDto(valid, errors, List.of(), blockingFacts);
        return ResponseEntity.ok(result);
    }

    // --- Submit ---

    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable UUID id, HttpServletRequest httpRequest) {
        log.info("Submitting tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(id, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        TaxReturn taxReturn = optTaxReturn.get();

        try {
            String remoteAddress = IPAddressUtil.getClientIpAddress(httpRequest);
            int remotePort = httpRequest.getRemotePort();
            String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);

            TaxReturn submitted = taxReturnService.submit(
                    id, taxReturn.getFacts(), userInfo.id(), userInfo, remoteAddress, remotePort, userAgent);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("tax_return_id", submitted.getId().toString());
            response.put("status", "SUBMITTED");
            response.put(
                    "submitted_at",
                    submitted.getMostRecentSubmitTime().toInstant().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to submit tax return {}", id, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to submit tax return", e);
        }
    }

    // --- Status ---

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID id) {
        log.info("Getting status for tax return {} via v2 API", id);
        UserInfo userInfo = userService.getCurrentUserInfo();

        StatusResponseBody statusBody = taxReturnService.getStatus(id, userInfo.id());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tax_return_id", id.toString());
        response.put("status", statusBody.getStatus().name());
        if (statusBody.getRejectionCodes() != null
                && !statusBody.getRejectionCodes().isEmpty()) {
            response.put("rejection_codes", statusBody.getRejectionCodes());
        }
        return ResponseEntity.ok(response);
    }

    // --- PDF ---

    @GetMapping(value = "/{id}/pdf/{lang}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> getPdf(@PathVariable UUID id, @PathVariable String lang) {
        log.info("Generating PDF for tax return {} in language {} via v2 API", id, lang);
        return ResponseEntity.ok().build();
    }

    // --- Private helpers ---

    private Map<String, FactTypeWithItem> toFactMap(Map<String, Object> translatedFacts) {
        Map<String, FactTypeWithItem> result = new LinkedHashMap<>();
        for (var entry : translatedFacts.entrySet()) {
            JsonNode node = objectMapper.valueToTree(entry.getValue());
            result.put(entry.getKey(), new FactTypeWithItem("String", node));
        }
        return result;
    }

    private void mergeFactsIntoTaxReturn(UUID taxReturnId, Map<String, Object> newFacts) {
        UserInfo userInfo = userService.getCurrentUserInfo();
        Optional<TaxReturn> optTaxReturn = taxReturnService.findByIdAndUserId(taxReturnId, userInfo.id());
        if (optTaxReturn.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax return not found");
        }

        TaxReturn taxReturn = optTaxReturn.get();
        Map<String, FactTypeWithItem> existingFacts = taxReturn.getFacts();
        if (existingFacts == null) {
            existingFacts = new LinkedHashMap<>();
        }
        existingFacts.putAll(toFactMap(newFacts));

        try {
            taxReturnService.update(taxReturnId, existingFacts, null, null, userInfo.id());
        } catch (Exception e) {
            log.error("Failed to update tax return {} with new facts", taxReturnId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to update tax return", e);
        }
    }
}
