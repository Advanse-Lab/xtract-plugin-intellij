package br.ufscar.advanse.xtractpluginintellij;

import java.util.List;

public class XtractApiExplanationInput {
    private String sourceCodeMethod;
    private String fragment;
    private Float score;
    private Float probability;
    private List<Feature> explanationRanking;

    public XtractApiExplanationInput(String sourceCodeMethod, String fragment, Float score, Float probability, List<Feature> explanationRanking) {
        this.sourceCodeMethod = sourceCodeMethod;
        this.fragment = fragment;
        this.score = score;
        this.probability = probability;
        this.explanationRanking = explanationRanking;
        System.out.println(this.explanationRanking.getFirst());
    }

    public String getSourceCodeMethod() {
        return sourceCodeMethod;
    }
    public String getFragment() {
        return fragment;
    }
    public Float getScore() {
        return score;
    }
    public Float getProbability() {
        return probability;
    }
    public List<Feature> getExplanationRanking() {
        return explanationRanking;
    }

    public void setSourceCodeMethod(String sourceCodeMethod) {
        this.sourceCodeMethod = sourceCodeMethod;
    }
    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
    public void setScore(Float score) {
        this.score = score;
    }
    public void setProbability(Float probability) {
        this.probability = probability;
    }
    public void setExplanationRanking(List<Feature> explanationRanking) {
        this.explanationRanking = explanationRanking;
    }
}

