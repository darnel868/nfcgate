package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.util.ArrayList;
import java.util.List;

/**
 * One BER-TLV node, as found in an EMV response. Read-only data holder.
 */
public class Tlv {
    public final String tagHex;      // e.g. "5F24"
    public final boolean constructed;
    public final byte[] value;       // raw value bytes (for a constructed node, the encoded children)
    public final List<Tlv> children = new ArrayList<>();

    public Tlv(String tagHex, boolean constructed, byte[] value) {
        this.tagHex = tagHex;
        this.constructed = constructed;
        this.value = value;
    }
}
