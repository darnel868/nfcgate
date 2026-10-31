package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EMV / ISO-7816 tag dictionary: human names, and which tags carry
 * cardholder data that must be redacted by default.
 */
public final class EmvTags {
    private EmvTags() {}

    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Set<String> SENSITIVE = new HashSet<>();

    static {
        put("6F", "File Control Information (FCI)");
        put("84", "Dedicated File (DF) Name");
        put("A5", "FCI Proprietary Template");
        put("88", "Short File Identifier (SFI)");
        put("50", "Application Label");
        put("87", "Application Priority Indicator");
        put("5F2D", "Language Preference");
        put("9F11", "Issuer Code Table Index");
        put("9F12", "Application Preferred Name");
        put("BF0C", "FCI Issuer Discretionary Data");
        put("61", "Application Template");
        put("4F", "Application Identifier (AID)");
        put("82", "Application Interchange Profile (AIP)");
        put("94", "Application File Locator (AFL)");
        put("9F38", "Processing Options Data Object List (PDOL)");
        put("70", "Record Template");
        put("77", "Response Message Template (Format 2)");
        put("80", "Response Message Template (Format 1)");
        put("5F25", "Application Effective Date");
        put("5F28", "Issuer Country Code");
        put("5F30", "Service Code");
        put("9F07", "Application Usage Control");
        put("9F08", "Application Version Number");
        put("9F42", "Application Currency Code");
        put("8C", "Card Risk Management DOL 1 (CDOL1)");
        put("8D", "Card Risk Management DOL 2 (CDOL2)");
        put("8E", "Cardholder Verification Method (CVM) List");
        put("8F", "Certification Authority Public Key Index");
        put("90", "Issuer Public Key Certificate");
        put("9F32", "Issuer Public Key Exponent");
        put("92", "Issuer Public Key Remainder");
        put("93", "Signed Static Application Data");
        put("9F46", "ICC Public Key Certificate");
        put("9F47", "ICC Public Key Exponent");
        put("9F48", "ICC Public Key Remainder");
        put("9F4A", "Static Data Authentication Tag List");
        put("9F36", "Application Transaction Counter (ATC)");
        put("9F26", "Application Cryptogram");
        put("9F27", "Cryptogram Information Data");
        put("9F10", "Issuer Application Data");
        put("9F37", "Unpredictable Number");

        // --- sensitive: cardholder data, redacted by default ---
        put("5A", "Application PAN", true);
        put("57", "Track 2 Equivalent Data", true);
        put("56", "Track 1 Data", true);
        put("5F20", "Cardholder Name", true);
        put("5F24", "Application Expiration Date", true);
        put("5F34", "PAN Sequence Number", true);
        put("9F1F", "Track 1 Discretionary Data", true);
        put("9F20", "Track 2 Discretionary Data", true);
        put("9F0B", "Cardholder Name Extended", true);
    }

    private static void put(String tag, String name) {
        NAMES.put(tag, name);
    }

    private static void put(String tag, String name, boolean sensitive) {
        NAMES.put(tag, name);
        if (sensitive)
            SENSITIVE.add(tag);
    }

    public static String name(String tagHex) {
        String n = NAMES.get(tagHex.toUpperCase());
        return n != null ? n : "Unknown tag";
    }

    public static boolean isSensitive(String tagHex) {
        return SENSITIVE.contains(tagHex.toUpperCase());
    }
}
