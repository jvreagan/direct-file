package gov.irs.directfile.api.forms.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxReturnSummaryDto(
        UUID taxReturnId,
        FilingStatusDto filingStatus,
        BigDecimal grossIncome,
        BigDecimal adjustmentsToIncome,
        BigDecimal agi,
        BigDecimal deductions,
        BigDecimal taxableIncome,
        BigDecimal tentativeTax,
        BigDecimal nonRefundableCredits,
        BigDecimal totalTax,
        BigDecimal totalWithholding,
        BigDecimal totalPayments,
        BigDecimal refundOrOwed,
        boolean dueRefund,
        String status) {}
