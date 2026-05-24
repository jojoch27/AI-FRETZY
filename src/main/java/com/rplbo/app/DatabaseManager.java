package com.rplbo.app;

public class DatabaseManager {
    private static final Database DATABASE = new Database();

    public static void setupDatabase() {
        DATABASE.setupDatabase();
    }

    public static void importKatalogDariJSON(String namaFile) {
        DATABASE.importKatalogDariJSON(namaFile);
    }

    public static String cariGitar(String keyword) {
        Chatbot chatbot = new Chatbot(DATABASE);
        return chatbot.searchProduct(keyword);
    }
}
