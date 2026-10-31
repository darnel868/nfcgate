package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.nio.charset.StandardCharsets;

/**
 * Recognizes command APDUs (reader -> card) and response status words.
 * Recognition only; nothing here constructs or sends APDUs.
 */
public final class ApduInfo {
    private ApduInfo() {}

    /** Friendly name for a command APDU, or null if not recognized. */
    public static String commandName(byte[] apdu) {
        if (apdu == null || apdu.length < 4)
            return null;
        int ins = apdu[1] & 0xFF, p1 = apdu[2] & 0xFF, p2 = apdu[3] & 0xFF;
        switch (ins) {
            case 0xA4:
                return isPpse(apdu) ? "SELECT PPSE" : "SELECT AID";
            case 0xA8:
                return "GET PROCESSING OPTIONS";
            case 0xB2:
                return "READ RECORD (SFI " + (p2 >> 3) + ", rec " + p1 + ")";
            case 0xCA:
                return "GET DATA";
            case 0x88:
                return "INTERNAL AUTHENTICATE";
            case 0xAE:
                return "GENERATE AC";
            case 0x82:
                return "EXTERNAL AUTHENTICATE";
            case 0x84:
                return "GET CHALLENGE";
            case 0x20:
                return "VERIFY";
            default:
                return null;
        }
    }

    private static boolean isPpse(byte[] apdu) {
        String s = new String(apdu, StandardCharsets.ISO_8859_1);
        return s.contains("2PAY.SYS.DDF01") || s.contains("1PAY.SYS.DDF01");
    }

    /** Last two bytes of a response as a 4-hex-digit status word, or null. */
    public static String statusWord(byte[] resp) {
        if (resp == null || resp.length < 2)
            return null;
        int sw1 = resp[resp.length - 2] & 0xFF, sw2 = resp[resp.length - 1] & 0xFF;
        return String.format("%02X%02X", sw1, sw2);
    }

    public static String statusMeaning(String sw) {
        if (sw == null)
            return "";
        switch (sw) {
            case "9000": return "OK";
            case "6A82": return "File or application not found";
            case "6A83": return "Record not found";
            case "6700": return "Wrong length";
            case "6D00": return "Instruction not supported";
            case "6A86": return "Incorrect P1/P2";
            case "6985": return "Conditions not satisfied";
            default:
                if (sw.startsWith("61"))
                    return "OK, more data available";
                if (sw.startsWith("6C"))
                    return "Wrong Le";
                return "";
        }
    }
}
