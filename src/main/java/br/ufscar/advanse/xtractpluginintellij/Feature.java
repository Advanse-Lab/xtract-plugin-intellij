package br.ufscar.advanse.xtractpluginintellij;

public class Feature {
    private String featureName;
    private String description;
    private float percentage;
    private float importanceValue;
    private int featureValue;

    public Feature(String featureName, String description, float percentage, float importanceValue, int featureValue) {
        this.featureName = featureName;
        this.description = description;
        this.percentage = percentage;
        this.importanceValue = importanceValue;
        this.featureValue = featureValue;
    }

    public String getFeatureName() {
        return featureName;
    }
    public String getDescription() {
        return description;
    }
    public float getPercentage() {
        return percentage;
    }
    public float getImportanceValue() {
        return importanceValue;
    }
    public int getFeatureValue() {
        return featureValue;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }
    public void setImportanceValue(float importanceValue) {
        this.importanceValue = importanceValue;
    }
    public void setFeatureValue(int featureValue) {
        this.featureValue = featureValue;
    }
}