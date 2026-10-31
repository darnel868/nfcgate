package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit tests for the read-only EMV dissector. Pure JVM, no Android. */
public class EmvDissectorTest {

    private static byte[] hex(String s) {
        s = s.replaceAll("\\s", "");
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return b;
    }

    private static DissectNode find(DissectNode n, String tag) {
        if (tag.equalsIgnoreCase(n.tagHex))
            return n;
        for (DissectNode c : n.children) {
            DissectNode r = find(c, tag);
            if (r != null)
                return r;
        }
        return null;
    }

    @Test
    public void recognizesSelectPpse() {
        DissectNode root = EmvDissector.describe(
                hex("00 A4 04 00 0E 325041592E5359532E4444463031 00"), false, true);
        assertEquals("SELECT PPSE", root.label);
    }

    @Test
    public void recognizesReadRecordWithSfi() {
        DissectNode root = EmvDissector.describe(hex("00 B2 01 0C 00"), false, true);
        assertEquals("READ RECORD (SFI 1, rec 1)", root.label);
    }

    @Test
    public void parsesFciTree() {
        DissectNode root = EmvDissector.describe(
                hex("6F 1A 84 07 A0000000041010 A5 0F 50 0A 4D617374657263617264 87 01 01 90 00"),
                true, true);
        assertTrue(root.label.contains("9000"));
        assertNotNull(find(root, "6F"));
        assertNotNull(find(root, "84"));
        DissectNode label = find(root, "50");
        assertNotNull(label);
        assertEquals("Mastercard", label.valueText);
    }

    @Test
    public void redactsCardholderData() {
        String rr = "70 1A 5A 08 4111111111111234 5F24 03 251231 5F20 05 4120434152 90 00";
        DissectNode root = EmvDissector.describe(hex(rr), true, true);

        DissectNode pan = find(root, "5A");
        assertNotNull(pan);
        assertTrue(pan.sensitive);
        assertTrue("PAN keeps last 4 digits", pan.valueHex.endsWith("12:34"));
        assertTrue("PAN is masked", pan.valueHex.contains("**"));
        assertNull("masked value exposes no ASCII", pan.valueText);

        DissectNode exp = find(root, "5F24");
        assertNotNull(exp);
        assertTrue(exp.valueHex.contains("**"));
    }

    @Test
    public void unredactedShowsFullValues() {
        String rr = "70 1A 5A 08 4111111111111234 5F24 03 251231 5F20 05 4120434152 90 00";
        DissectNode root = EmvDissector.describe(hex(rr), true, false);
        DissectNode pan = find(root, "5A");
        assertNotNull(pan);
        assertEquals("41:11:11:11:11:11:12:34", pan.valueHex);
    }

    @Test
    public void decodesStatusWord() {
        assertEquals("9000", ApduInfo.statusWord(hex("6F 02 84 00 90 00")));
        assertEquals("OK", ApduInfo.statusMeaning("9000"));
        assertEquals("Record not found", ApduInfo.statusMeaning("6A83"));
    }
}
