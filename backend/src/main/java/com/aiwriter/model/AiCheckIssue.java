package com.aiwriter.model;

public class AiCheckIssue {
    private String level;
    private String category;
    private String term;
    private int line;
    private String excerpt;
    private String suggestion;

    public AiCheckIssue() {
    }

    public AiCheckIssue(String level, String category, String term, int line, String excerpt, String suggestion) {
        this.level = level;
        this.category = category;
        this.term = term;
        this.line = line;
        this.excerpt = excerpt;
        this.suggestion = suggestion;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
