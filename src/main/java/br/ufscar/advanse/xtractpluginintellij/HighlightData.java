package br.ufscar.advanse.xtractpluginintellij;

import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.markup.TextAttributes;

public class HighlightData {
    int startOffset;
    int endOffset;
    TextAttributes attributes;
    RangeMarker marker;

    public HighlightData() {}

    public HighlightData(int start, int end, TextAttributes attrs) {
        this.startOffset = start;
        this.endOffset = end;
        this.attributes = attrs;
    }

    public HighlightData(int start, int end, TextAttributes attrs, RangeMarker marker) {
        this.startOffset = start;
        this.endOffset = end;
        this.attributes = attrs;
        this.marker = marker;
    }
}
