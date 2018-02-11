package com.indev.p2pmessenger.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
public class ClientApp extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(ClientApp.class, args);
    }

    @Override
    public void init() {
        springContext = SpringApplication.run(ClientApp.class);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/Chat.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Parent rootNode = fxmlLoader.load();

        primaryStage.setTitle("P2P Messenger");
        primaryStage.setScene(new Scene(rootNode));
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
    }
}
