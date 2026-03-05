package gov.irs.directfile.api.forms.translation;

import java.math.BigDecimal;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import gov.irs.directfile.api.forms.dto.*;

@Slf4j
@Service
public class FormTranslationService {

    public Map<String, Object> translateW2ToFacts(W2FormDto dto, UUID itemId) {
        Map<String, Object> facts = new LinkedHashMap<>();
        String id = itemId.toString();
        putIfNotNull(facts, String.format(FactPathConstants.W2_EMPLOYER_NAME, id), dto.employerName());
        putIfNotNull(facts, String.format(FactPathConstants.W2_EIN, id), dto.ein());
        putDollar(facts, String.format(FactPathConstants.W2_WAGES, id), dto.wages());
        putDollar(facts, String.format(FactPathConstants.W2_FED_WITHHOLDING, id), dto.federalWithholding());
        putDollar(facts, String.format(FactPathConstants.W2_SS_WAGES, id), dto.socialSecurityWages());
        putDollar(facts, String.format(FactPathConstants.W2_SS_TAX, id), dto.socialSecurityTaxWithheld());
        putDollar(facts, String.format(FactPathConstants.W2_MEDICARE_WAGES, id), dto.medicareWages());
        putDollar(facts, String.format(FactPathConstants.W2_MEDICARE_TAX, id), dto.medicareTaxWithheld());
        return facts;
    }

    public Map<String, Object> translate1099IntToFacts(Form1099IntDto dto, UUID itemId) {
        Map<String, Object> facts = new LinkedHashMap<>();
        String id = itemId.toString();
        putIfNotNull(facts, String.format(FactPathConstants.INT_PAYER, id), dto.payerName());
        putIfNotNull(facts, String.format(FactPathConstants.INT_PAYER_TIN, id), dto.payerTin());
        putDollar(facts, String.format(FactPathConstants.INT_1099_AMOUNT, id), dto.interestIncome());
        putDollar(facts, String.format(FactPathConstants.INT_TAX_WITHHELD, id), dto.federalTaxWithheld());
        putDollar(facts, String.format(FactPathConstants.INT_FOREIGN_TAX, id), dto.foreignTaxPaid());
        putDollar(facts, String.format(FactPathConstants.INT_TAX_EXEMPT, id), dto.taxExemptInterest());
        putDollar(facts, String.format(FactPathConstants.INT_EARLY_WITHDRAWAL, id), dto.earlyWithdrawalPenalty());
        putDollar(facts, String.format(FactPathConstants.INT_GOV_BONDS, id), dto.interestOnGovernmentBonds());
        facts.put(String.format(FactPathConstants.INT_HAS_1099, id), dto.has1099());
        return facts;
    }

    public Map<String, Object> translate1099DivToFacts(Form1099DivDto dto, UUID itemId) {
        Map<String, Object> facts = new LinkedHashMap<>();
        String id = itemId.toString();
        putIfNotNull(facts, String.format(FactPathConstants.DIV_PAYER_NAME, id), dto.payerName());
        putIfNotNull(facts, String.format(FactPathConstants.DIV_PAYER_TIN, id), dto.payerTin());
        putDollar(facts, String.format(FactPathConstants.DIV_ORDINARY, id), dto.ordinaryDividends());
        putDollar(facts, String.format(FactPathConstants.DIV_QUALIFIED, id), dto.qualifiedDividends());
        putDollar(facts, String.format(FactPathConstants.DIV_FED_WITHHELD, id), dto.federalTaxWithheld());
        return facts;
    }

    public Map<String, Object> translate1099BToFacts(Form1099BDto dto, UUID itemId) {
        Map<String, Object> facts = new LinkedHashMap<>();
        String id = itemId.toString();
        putIfNotNull(facts, String.format(FactPathConstants.B_BROKER_NAME, id), dto.brokerName());
        putIfNotNull(facts, String.format(FactPathConstants.B_DESCRIPTION, id), dto.description());
        if (dto.dateSold() != null) {
            facts.put(
                    String.format(FactPathConstants.B_DATE_SOLD, id),
                    dto.dateSold().toString());
        }
        putDollar(facts, String.format(FactPathConstants.B_PROCEEDS, id), dto.proceeds());
        putDollar(facts, String.format(FactPathConstants.B_COST_BASIS, id), dto.costBasis());
        return facts;
    }

    public Map<String, Object> translateFilerToFacts(FilerDto dto) {
        Map<String, Object> facts = new LinkedHashMap<>();
        putIfNotNull(facts, FactPathConstants.PRIMARY_FILER_FIRST_NAME, dto.firstName());
        putIfNotNull(facts, FactPathConstants.PRIMARY_FILER_LAST_NAME, dto.lastName());
        if (dto.dateOfBirth() != null) {
            facts.put(FactPathConstants.PRIMARY_FILER_DOB, dto.dateOfBirth().toString());
        }
        putIfNotNull(facts, FactPathConstants.PRIMARY_FILER_TIN, dto.ssn());
        return facts;
    }

    public Map<String, Object> translateAddressToFacts(AddressDto dto) {
        Map<String, Object> facts = new LinkedHashMap<>();
        putIfNotNull(facts, FactPathConstants.ADDRESS_STREET, dto.street());
        putIfNotNull(facts, FactPathConstants.ADDRESS_CITY, dto.city());
        putIfNotNull(facts, FactPathConstants.ADDRESS_STATE, dto.state());
        putIfNotNull(facts, FactPathConstants.ADDRESS_ZIP, dto.zip());
        return facts;
    }

    private void putIfNotNull(Map<String, Object> facts, String path, Object value) {
        if (value != null) {
            facts.put(path, value);
        }
    }

    private void putDollar(Map<String, Object> facts, String path, BigDecimal value) {
        if (value != null) {
            facts.put(path, value.toString());
        }
    }
}
