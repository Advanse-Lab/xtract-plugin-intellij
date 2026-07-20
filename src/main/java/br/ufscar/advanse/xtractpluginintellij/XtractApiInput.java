package br.ufscar.advanse.xtractpluginintellij;

public class XtractApiInput {
    private String simplifiedSlicesEncoded;
    private String methodMetricsEncoded;

    public XtractApiInput(String simplifiedSlicesEncoded, String methodMetricsEncoded) {
        this.simplifiedSlicesEncoded = simplifiedSlicesEncoded;
        this.methodMetricsEncoded = methodMetricsEncoded;
    }
}
