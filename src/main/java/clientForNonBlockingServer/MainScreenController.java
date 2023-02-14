package clientForNonBlockingServer;



import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import clientForNonBlockingServer.client.Socket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MainScreenController {

    @FXML
    private TextField port;

    @FXML
    private Button sendParameters;

    @FXML
    private Button sendTextToSend;

    @FXML
    private TextArea textArea;

    @FXML
    private TextField textToSend;

    @FXML
    private TextField userName;
    
    @FXML
    private Button getAllUsers;
    
    @FXML
    private ComboBox<String> usersCB;
    
    
    @FXML
    private Button logIn;

    @FXML
    private Button newUser;

    @FXML
    private PasswordField password;
    
    @FXML
    private Button disconnect;

    
    private Socket socket = null;
    
    
    
	@FXML
	private void initialize() {
		
		sendParameters.setOnAction(ae -> sendParameters());
		
		getAllUsers.setOnAction(ae -> getAllUsers());
		
		logIn.setOnAction(ae -> logIn());
		
		newUser.setOnAction(ae -> newUser());
		
		sendTextToSend.setOnAction( ae -> sendTextMessage());
		
		disconnect.setOnAction(ae -> disconnect());
		

	}
	

	

	
	
	
	private void sendParameters() {
		textArea.appendText("Let's start");
		
		socket = new Socket (Integer.parseInt(port.getText()), 
				string -> {Platform.runLater (() -> textArea.appendText(string + System.lineSeparator()));},
				allUsers -> {Platform.runLater(() -> updateAllUsers(allUsers));});
		
		new Thread(socket).start();
		
	}
	
	private void getAllUsers() {
			socket.getAllUsers();
		
	}
	
	private void newUser() {
		socket.newUser(userName.getText(), password.getText().getBytes(StandardCharsets.UTF_8));
	}
	
	private void logIn() {
		socket.tryLogIn(userName.getText(), password.getText().getBytes(StandardCharsets.UTF_8));
	}
	
	private void sendTextMessage() {
		socket.sendTextMessage(usersCB.getSelectionModel().getSelectedItem(), textToSend.getText());
	}
	
	private void disconnect() {
		socket.stopMe();
	}
	

	public void stopSocket() {
		socket.stopMe();
	}
	
	public void updateAllUsers(String[] allUsers) {
		usersCB.getItems().clear();
		usersCB.getItems().addAll(allUsers);
	}

}