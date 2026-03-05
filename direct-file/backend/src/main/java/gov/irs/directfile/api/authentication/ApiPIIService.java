package gov.irs.directfile.api.authentication;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import gov.irs.directfile.api.config.BeanProfiles;
import gov.irs.directfile.models.encryption.EncryptionService;

@Slf4j
@Service
@Profile("!" + BeanProfiles.FAKE_PII_SERVICE)
public class ApiPIIService implements PIIService {
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern ITIN_PATTERN = Pattern.compile("^9\\d{8}$");
    private static final Pattern EIN_PATTERN = Pattern.compile("^\\d{9}$");

    private final EncryptionService encryptionService;
    private final Map<UUID, Map<PIIAttribute, String>> piiStore = new ConcurrentHashMap<>();

    public ApiPIIService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public Map<PIIAttribute, String> fetchAttributes(UUID userExternalId, Set<PIIAttribute> attributes) {
        Map<PIIAttribute, String> storedPii = piiStore.getOrDefault(userExternalId, Map.of());
        Map<PIIAttribute, String> result = new HashMap<>();
        for (PIIAttribute attr : attributes) {
            String value = storedPii.get(attr);
            if (value != null) {
                result.put(attr, value);
            }
        }
        return result;
    }

    public void storePII(UUID userExternalId, PIIAttribute attribute, String value) {
        validatePII(attribute, value);
        byte[] encrypted = encryptionService.encrypt(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                Map.of("userId", userExternalId.toString(), "attribute", attribute.name()));
        log.debug("Encrypted PII attribute {} ({} bytes)", attribute, encrypted.length);

        piiStore.computeIfAbsent(userExternalId, k -> new ConcurrentHashMap<>()).put(attribute, value);
        log.info("Stored PII attribute {} for user {}", attribute, userExternalId);
    }

    public void storePII(UUID userExternalId, Map<PIIAttribute, String> attributes) {
        attributes.forEach((attr, value) -> storePII(userExternalId, attr, value));
    }

    public static String maskTin(String tin) {
        if (tin == null || tin.length() < 4) return "***";
        return "***-**-" + tin.substring(tin.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    public static void validateTin(String tin) {
        if (tin == null || tin.isBlank()) {
            throw new PIIValidationException("TIN is required");
        }
        String cleaned = tin.replaceAll("[\\s-]", "");
        if (!SSN_PATTERN.matcher(cleaned).matches()
                && !ITIN_PATTERN.matcher(cleaned).matches()) {
            throw new PIIValidationException(
                    "Invalid TIN format. Must be a valid SSN (9 digits) or ITIN (9xx-xx-xxxx)");
        }
    }

    public static void validateEin(String ein) {
        if (ein == null || ein.isBlank()) {
            throw new PIIValidationException("EIN is required");
        }
        String cleaned = ein.replaceAll("[\\s-]", "");
        if (!EIN_PATTERN.matcher(cleaned).matches()) {
            throw new PIIValidationException("Invalid EIN format. Must be 9 digits");
        }
    }

    private void validatePII(PIIAttribute attribute, String value) {
        switch (attribute) {
            case TIN -> validateTin(value);
            default -> {
                if (value == null || value.isBlank()) {
                    throw new PIIValidationException(attribute.name() + " value is required");
                }
            }
        }
    }

    public static class PIIValidationException extends RuntimeException {
        public PIIValidationException(String message) {
            super(message);
        }
    }
}
