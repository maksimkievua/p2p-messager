package com.indev.p2pmessenger.client.controller;

import com.indev.p2pmessenger.client.service.ChatService;
import com.indev.p2pmessenger.protocol.dto.ChatMessage;
import com.indev.p2pmessenger.protocol.dto.Contact;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.ContactAppeared;
import com.indev.p2pmessenger.protocol.dto.network.message.discovery.ContactDisappeared;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatController implements Initializable {
    private final Map<String, ChatState> chats = new HashMap<>();

    private final ChatService chatService;
    @FXML
    private Button loginButton;
    @FXML
    private Pane loginPane;
    @FXML
    private TextField loginTextField;
    @FXML
    private Label userInfoLabel;

    @FXML
    private ListView<Contact> contacts;
    @FXML
    private Label chatLabel;
    @FXML
    private Pane chatBox;
    @FXML
    private Pane messagesBox;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private Button sendButton;

    private Contact openChatContact;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        contacts.setCellFactory(param -> new ListCell<Contact>() {
            @Override
            protected void updateItem(Contact item, boolean empty) {
                super.updateItem(item, empty);
                setText(item != null ? item.getName() : "");
            }
        });
        contacts.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> renderChat(newValue));
        loginTextField.textProperty().addListener((observable, oldValue, newValue) ->
                loginButton.setDisable(newValue.trim().isEmpty()));
        messageTextArea.textProperty().addListener((observable, oldValue, newValue) ->
                sendButton.setDisable(newValue.trim().isEmpty()));
    }

    private void renderContacts(List<Contact> contacts) {
        List<Contact> filteredContacts = contacts.stream()
                .filter(contact -> !contact.getName().equals(chatService.getLogin()))
                .collect(toList());
        this.contacts.setItems(observableArrayList(filteredContacts));
    }

    private void renderChat(Contact contact) {
        openChatContact = contact;
        ChatState chatState = getChatState(contact.getName());
        chatLabel.setText("Chat with " + contact.getName());

        messagesBox.getChildren().clear();
        chatState.getChatMessages().forEach(chatMessage -> messagesBox.getChildren().add(toMessageBox(chatMessage)));

        chatBox.setVisible(true);
        sendButton.setDisable(true);
    }

    private ChatState getChatState(String contactName) {
        return chats.computeIfAbsent(contactName, s -> new ChatState(new ArrayList<>()));
    }

    private Pane toMessageBox(ChatMessage chatMessage) {
        return Objects.equals(chatService.getLogin(), chatMessage.getFrom()) ?
                messageBoxFromMe(chatMessage.getTimestamp(), chatMessage.getMessage()) :
                messageBoxFromUser(chatMessage.getFrom(), chatMessage.getTimestamp(), chatMessage.getMessage());
    }

    private Pane messageBoxFromUser(String name, long timestamp, String message) {
        return messageBox(name, timestamp, message, Color.BLUE);
    }

    private Pane messageBoxFromMe(long timestamp, String message) {
        return messageBox(chatService.getLogin(), timestamp, message, Color.RED);
    }

    private Pane messageBox(String name, long timestamp, String message, Color color) {
        Text header = new Text(name + " (" + formatDate(timestamp) + "): ");
        header.setFill(color);
        return new VBox(
                header,
                new Text(message)
        );
    }

    private String formatDate(Long timestamp) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
                .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC));
    }

    @FXML
    public void sendMessage() {
        chatService.sendMessage(openChatContact, messageTextArea.getText());
        messageTextArea.clear();
    }

    @FXML
    public void login() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);

        chatService.login(loginTextField.getText())
                .thenAccept(aVoid -> Platform.runLater(() -> {
                    userInfoLabel.setVisible(true);
                    userInfoLabel.setText("Logged in as: " + chatService.getLogin());
                }))
                .thenCompose(aVoid -> {
                    chatService.addDiscoveryEventListener(this::onDiscoveryEvent);
                    chatService.addMessageListener(this::onMessage);
                    return chatService.loadContacts();
                })
                .thenAccept(contacts -> Platform.runLater(() -> renderContacts(contacts)));
    }

    private void onMessage(ChatMessage chatMessage) {
        Platform.runLater(() -> {
            if (openChatContact != null && messageRelatesToContact(chatMessage, openChatContact.getName())) {
                messagesBox.getChildren().add(toMessageBox(chatMessage));
            }
            ChatState chatState = getChatState(chatMessage.getFrom());
            chatState.getChatMessages().add(chatMessage);
        });
    }

    private boolean messageRelatesToContact(ChatMessage chatMessage, String contactName) {
        return chatMessage.getFrom().equals(contactName) || chatMessage.getTo().equals(contactName);
    }

    private void onDiscoveryEvent(Object event) {
        if (event instanceof ContactAppeared) {
            Platform.runLater(() -> onContactAppeared((ContactAppeared) event));
        } else if (event instanceof ContactDisappeared) {
            Platform.runLater(() -> onContactDisappeared((ContactDisappeared) event));
        }
    }

    private void onContactDisappeared(ContactDisappeared event) {
        contacts.getItems().remove(event.getContact());
        if (event.getContact().equals(openChatContact)) {
            messagesBox.setVisible(false);
        }
    }

    private void onContactAppeared(ContactAppeared event) {
        contacts.getItems().add(event.getContact());
    }

    @Value
    private static class ChatState {
        private List<ChatMessage> chatMessages;
    }
}
