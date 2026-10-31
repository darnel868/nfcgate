package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.util.Arrays;
import java.util.List;

import de.tu_darmstadt.seemoo.nfcgate.util.NfcComm;
import de.tu_darmstadt.seemoo.nfcgate.util.Utils;

/**
 * Read-only EMV dissector. Given a captured APDU exchange it produces a
 * human-readable tree of {@link DissectNode}. It observes and explains only:
 * it never builds, modifies, or emits an APDU, and it cannot emulate a card.
 *
 * Cardholder data (PAN, track data, expiry, name) is redacted by default.
 * Pass redact=false only for local analysis of a card you own.
 */
public final class EmvDissector {
    private EmvDissector() {}

    /** Convenience for a captured communication. */
    public static DissectNode describe(NfcComm comm, boolean redact) {
        return describe(comm.getData(), comm.isCard(), redact);
    }

    /**
     * @param data     the raw APDU bytes
     * @param fromCard true if this is a card response, false for a reader command
     * @param redact   mask sensitive cardholder values
     */
    public static DissectNode describe(byte[] data, boolean fromCard, boolean redact) {
        if (!fromCard) {
            String cmd = ApduInfo.commandName(data);
            return new DissectNode(null, cmd != null ? cmd : "Command APDU",
                    data != null ? Utils.bytesToHex(data) : null, null, false, 0);
        }

        String sw = ApduInfo.statusWord(data);
        String meaning = ApduInfo.statusMeaning(sw);
        StringBuilder label = new StringBuilder("Response");
        if (sw != null) {
            label.append(" (").append(sw);
            if (!meaning.isEmpty())
                label.append(" - ").append(meaning);
            label.append(")");
        }
        DissectNode root = new DissectNode(null, label.toString(), null, null, false, 0);

        // parse TLVs over the response body, excluding a trailing 2-byte status word
        byte[] body = data;
        if (data != null && data.length >= 2)
            body = Arrays.copyOf(data, data.length - 2);

        List<Tlv> tlvs = TlvParser.parse(body);
        for (Tlv t : tlvs)
            addNode(root, t, redact, 1);
        return root;
    }

    private static void addNode(DissectNode parent, Tlv t, boolean redact, int depth) {
        boolean sensitive = EmvTags.isSensitive(t.tagHex);
        String name = EmvTags.name(t.tagHex);
        String valueHex = null, valueText = null;

        if (!t.constructed) {
            if (sensitive && redact) {
                valueHex = maskHex(t.tagHex, t.value);
            } else {
                valueHex = Utils.bytesToHex(t.value);
                valueText = asciiIfPrintable(t.value);
            }
        }

        DissectNode node = new DissectNode(t.tagHex, name, valueHex, valueText, sensitive, depth);
        parent.children.add(node);

        if (t.constructed)
            for (Tlv c : t.children)
                addNode(node, c, redact, depth + 1);
    }

    /** Mask a sensitive value. For the PAN (5A) the last 2 bytes are kept. */
    private static String maskHex(String tagHex, byte[] value) {
        int keep = "5A".equalsIgnoreCase(tagHex) ? 2 : 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length; i++) {
            if (i >= value.length - keep)
                sb.append(String.format("%02X", value[i] & 0xFF));
            else
                sb.append("**");
            if (i < value.length - 1)
                sb.append(':');
        }
        return sb.toString();
    }

    private static String asciiIfPrintable(byte[] v) {
        if (v == null || v.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : v) {
            int c = b & 0xFF;
            if (c < 0x20 || c > 0x7E)
                return null;
            sb.append((char) c);
        }
        return sb.toString();
    }
}
