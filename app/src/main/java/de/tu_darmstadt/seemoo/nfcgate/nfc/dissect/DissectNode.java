package de.tu_darmstadt.seemoo.nfcgate.nfc.dissect;

import java.util.ArrayList;
import java.util.List;

/** One row of decoded output: a tag/value or a command/response header. */
public class DissectNode {
    public final String tagHex;     // nullable (null for command/response headers)
    public final String label;      // tag name, or command / response description
    public final String valueHex;   // nullable raw (or masked) hex
    public final String valueText;  // nullable decoded ASCII, when printable
    public final boolean sensitive; // true if this value was (or would be) redacted
    public final int depth;         // nesting level for indentation
    public final List<DissectNode> children = new ArrayList<>();

    public DissectNode(String tagHex, String label, String valueHex, String valueText,
                       boolean sensitive, int depth) {
        this.tagHex = tagHex;
        this.label = label;
        this.valueHex = valueHex;
        this.valueText = valueText;
        this.sensitive = sensitive;
        this.depth = depth;
    }
}
