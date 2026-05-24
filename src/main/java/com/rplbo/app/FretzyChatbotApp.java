package com.rplbo.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FretzyChatbotApp extends Application {
    @Override
    public void init() {
        Database database = new Database();
        database.setupDatabase();
        database.importKatalogDariJSON("guitar_data.json");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(FretzyChatbotApp.class.getResource("/com/rplbo/app/MainShell.fxml"));
        Scene scene = new Scene(root, 1120, 720);
        primaryStage.setTitle("Fretzy Bot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
