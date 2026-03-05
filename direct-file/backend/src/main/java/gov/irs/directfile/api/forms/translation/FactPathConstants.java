package gov.irs.directfile.api.forms.translation;

public final class FactPathConstants {
    private FactPathConstants() {}

    // Filer paths
    public static final String PRIMARY_FILER_FIRST_NAME = "/primaryFiler/firstName";
    public static final String PRIMARY_FILER_LAST_NAME = "/primaryFiler/lastName";
    public static final String PRIMARY_FILER_DOB = "/primaryFiler/dateOfBirth";
    public static final String PRIMARY_FILER_TIN = "/primaryFiler/tin";
    public static final String SECONDARY_FILER_FIRST_NAME = "/secondaryFiler/firstName";
    public static final String SECONDARY_FILER_LAST_NAME = "/secondaryFiler/lastName";

    // Filing status
    public static final String FILING_STATUS = "/filingStatus";

    // Address
    public static final String ADDRESS_STREET = "/address/streetAddress";
    public static final String ADDRESS_CITY = "/address/city";
    public static final String ADDRESS_STATE = "/address/state";
    public static final String ADDRESS_ZIP = "/address/zipCode";

    // W-2 collection
    public static final String FORM_W2S = "/formW2s";
    public static final String W2_EMPLOYER_NAME = "/formW2s/#%s/employerName";
    public static final String W2_EIN = "/formW2s/#%s/employerEin";
    public static final String W2_WAGES = "/formW2s/#%s/wages";
    public static final String W2_FED_WITHHOLDING = "/formW2s/#%s/federalWithholding";
    public static final String W2_SS_WAGES = "/formW2s/#%s/socialSecurityWages";
    public static final String W2_SS_TAX = "/formW2s/#%s/socialSecurityTaxWithheld";
    public static final String W2_MEDICARE_WAGES = "/formW2s/#%s/medicareWages";
    public static final String W2_MEDICARE_TAX = "/formW2s/#%s/medicareTaxWithheld";

    // 1099-INT collection
    public static final String INTEREST_REPORTS = "/interestReports";
    public static final String INT_PAYER = "/interestReports/#%s/payer";
    public static final String INT_PAYER_TIN = "/interestReports/#%s/payer/tin";
    public static final String INT_1099_AMOUNT = "/interestReports/#%s/writable1099Amount";
    public static final String INT_TAX_WITHHELD = "/interestReports/#%s/writableTaxWithheld";
    public static final String INT_FOREIGN_TAX = "/interestReports/#%s/writableForeignTaxPaid";
    public static final String INT_TAX_EXEMPT = "/interestReports/#%s/writableTaxExemptInterest";
    public static final String INT_HAS_1099 = "/interestReports/#%s/has1099";
    public static final String INT_EARLY_WITHDRAWAL = "/interestReports/#%s/writableEarlyWithdrawlPenaltyAmount";
    public static final String INT_GOV_BONDS = "/interestReports/#%s/writableInterestOnGovernmentBonds";

    // 1099-DIV collection
    public static final String FORM_1099_DIVS = "/form1099Divs";
    public static final String DIV_PAYER_NAME = "/form1099Divs/#%s/payerName";
    public static final String DIV_PAYER_TIN = "/form1099Divs/#%s/payerTin";
    public static final String DIV_ORDINARY = "/form1099Divs/#%s/ordinaryDividends";
    public static final String DIV_QUALIFIED = "/form1099Divs/#%s/qualifiedDividends";
    public static final String DIV_FED_WITHHELD = "/form1099Divs/#%s/federalTaxWithheld";

    // 1099-B collection
    public static final String FORM_1099_BS = "/form1099Bs";
    public static final String B_BROKER_NAME = "/form1099Bs/#%s/brokerName";
    public static final String B_DESCRIPTION = "/form1099Bs/#%s/description";
    public static final String B_DATE_SOLD = "/form1099Bs/#%s/dateSold";
    public static final String B_PROCEEDS = "/form1099Bs/#%s/proceeds";
    public static final String B_COST_BASIS = "/form1099Bs/#%s/costBasis";

    // Schedule C collection
    public static final String SCHEDULE_CS = "/scheduleCs";
    public static final String SC_BUSINESS_NAME = "/scheduleCs/#%s/businessName";
    public static final String SC_GROSS_RECEIPTS = "/scheduleCs/#%s/grossReceipts";

    // Summary paths (derived facts)
    public static final String TOTAL_INCOME = "/totalIncome";
    public static final String AGI = "/agi";
    public static final String TAXABLE_INCOME = "/taxableIncome";
    public static final String TOTAL_TAX = "/totalTax";
    public static final String TOTAL_PAYMENTS = "/totalPayments";
    public static final String OVERPAYMENT = "/overpayment";
    public static final String BALANCE_DUE = "/balanceDue";
    public static final String DUE_REFUND = "/dueRefund";
    public static final String ADJUSTMENTS_TO_INCOME = "/adjustmentsToIncome";
    public static final String TOTAL_DEDUCTIONS = "/totalDeductions";
    public static final String TENTATIVE_TAX = "/tentativeTaxFromTaxableIncome";
    public static final String NON_REFUNDABLE_CREDITS = "/nonRefundableCredits";
    public static final String TOTAL_WITHHOLDING = "/totalWithholding";
}
