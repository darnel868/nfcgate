package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, defensive BER-TLV parser for EMV responses. Read-only: it never
 * modifies input and never emits anything, it only decodes structure.
 */
public final class TlvParser {
    private TlvParser() {}

    public static List<Tlv> parse(byte[] data) {
        List<Tlv> out = new ArrayList<>();
        if (data != null)
            parse(data, 0, data.length, out);
        return out;
    }

    private static void parse(byte[] data, int start, int end, List<Tlv> out) {
        int i = start;
        while (i < end) {
            int b0 = data[i] & 0xFF;
            // skip padding bytes between TLVs
            if (b0 == 0x00 || b0 == 0xFF) {
                i++;
                continue;
            }

            // --- tag ---
            int tagStart = i;
            boolean constructed = (b0 & 0x20) != 0;
            i++;
            if ((b0 & 0x1F) == 0x1F) {
                // multi-byte tag: continue while high bit set
                while (i < end && (data[i] & 0x80) != 0)
                    i++;
                if (i < end)
                    i++;
            }
            int tagEnd = i;
            if (tagEnd > end)
                break;
            StringBuilder tagHex = new StringBuilder();
            for (int t = tagStart; t < tagEnd; t++)
                tagHex.append(String.format("%02X", data[t] & 0xFF));

            // --- length ---
            if (i >= end)
                break;
            int lenByte = data[i] & 0xFF;
            i++;
            int length;
            if ((lenByte & 0x80) == 0) {
                length = lenByte;
            } else {
                int numBytes = lenByte & 0x7F;
                if (numBytes == 0 || numBytes > 4 || i + numBytes > end)
                    break;
                length = 0;
                for (int n = 0; n < numBytes; n++) {
                    length = (length << 8) | (data[i] & 0xFF);
                    i++;
                }
            }
            if (length < 0 || i + length > end)
                length = Math.max(0, end - i); // truncated; clamp instead of throwing

            byte[] value = new byte[length];
            System.arraycopy(data, i, value, 0, length);
            i += length;

            Tlv node = new Tlv(tagHex.toString(), constructed, value);
            if (constructed)
                parse(value, 0, value.length, node.children);
            out.add(node);
        }
    }
}
