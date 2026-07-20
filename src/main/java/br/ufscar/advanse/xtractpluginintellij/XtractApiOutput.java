package br.ufscar.advanse.xtractpluginintellij;

import java.util.List;

public class XtractApiOutput {
    private String SourceCodeMethod;
    private String fragment;
    private int beginLine;
    private int beginColumn;
    private int endLine;
    private int endColumn;
    private String refactoredCode;
    private Float UseMetric;
    private Float score;
    private Float probability;
    private Float forestPrediction;
    private String explicacao;
    private List<Feature> rankingExplanation;

    public XtractApiOutput(String SourceCodeMethod, String fragment, int beginLine, int beginColumn, int endLine, int endColumn, String refactoredCode, Float UseMetric, Float score, Float probability, Float forestPrediction, List<Feature> rankingExplanation) {
        this.SourceCodeMethod = SourceCodeMethod;
        this.fragment = fragment;
        this.beginLine = beginLine;
        this.beginColumn = beginColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.UseMetric = UseMetric;
        this.score = score;
        this.probability = probability;
        this.forestPrediction = forestPrediction;
        this.refactoredCode = refactoredCode;
        this.rankingExplanation = rankingExplanation;
        System.out.println(this.rankingExplanation);
    }

    public XtractApiOutput(Float score, Float probability, String explicacao) {
        this.score = score;
        this.probability = probability;
        this.explicacao = explicacao;
    }

    public XtractApiOutput(String fragment, Float score, Float probability, String explicacao, List<Feature> rankingExplanation, Float forestPrediction) {
        this.fragment = fragment;
        this.score = score;
        this.probability = probability;
        this.explicacao = explicacao;
        this.rankingExplanation = rankingExplanation;
        this.forestPrediction = forestPrediction;
    }

    // Textual explanation from API
    public XtractApiOutput(String explicacao) {
        this.explicacao = explicacao;
    }

//    public ApiOutput(String SourceCodeMethod, String fragment, Float UseMetric, Float score, Float probability, Float forestPrediction, String explicacao) {
//        this.SourceCodeMethod = SourceCodeMethod;
//        this.fragment = fragment;
//        this.UseMetric = UseMetric;
//        this.score = score;
//        this.probability = probability;
//        this.forestPrediction = forestPrediction;
//        this.explicacao = explicacao;
//    }

    public String getSourceCodeMethod() {
        return SourceCodeMethod;
    }
    public String getFragment() {
        return fragment;
    }
    public int getBeginLine() {
        return beginLine;
    }
    public int getBeginColumn() {
        return beginColumn;
    }
    public int getEndLine() {
        return endLine;
    }
    public int getEndColumn() {
        return endColumn;
    }
    public String getRefactoredCode() {
        return refactoredCode;
    }
    public Float getScore() {
        return score;
    }
    public Float getProbability() {
        return probability;
    }
    public Float getForestPrediction() {
        return forestPrediction;
    }
    public String getExplicacao() {
        return explicacao;
    }
    public List<Feature> getRankingExplanation() {
        return rankingExplanation;
    }

    public boolean isForestPredictionValid() {
        return (forestPrediction != null);
    }

    public void setSourceCodeMethod(String sourceCodeMethod) {
        SourceCodeMethod = sourceCodeMethod;
    }
    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
    public void setBeginLine(int beginLine) {
        this.beginLine = beginLine;
    }
    public void setBeginColumn(int beginColumn) {
        this.beginColumn = beginColumn;
    }
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }
    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }
    public void setRefactoredCode(String refactoredCode) {
        this.refactoredCode = refactoredCode;
    }
    public void setScore(Float score) {
        this.score = score;
    }
    public void setProbability(Float probability) {
        this.probability = probability;
    }
    public void setForestPrediction(Float forestPrediction) {
        this.forestPrediction = forestPrediction;
    }
    public void setExplicacao(String explicacao) {
        this.explicacao = explicacao;
    }
    public void setRankingExplanation(List<Feature> rankingExplanation) {
        this.rankingExplanation = rankingExplanation;
    }
}
