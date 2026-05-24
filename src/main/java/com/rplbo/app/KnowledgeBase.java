package com.rplbo.app;

public class KnowledgeBase {
    private final String keyword;
    private final String response;

    public KnowledgeBase(String keyword, String response) {
        this.keyword = keyword;
        this.response = response;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getResponse() {
        return response;
    }

    public boolean searchAnswer(String input) {
        return normalize(input).contains(normalize(keyword));
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replace("?", " ")
                .replace("!", " ")
                .replace(".", " ")
                .replace(",", " ")
                .trim();
    }
}
