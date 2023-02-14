package clientForNonBlockingServer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MainClass extends Application {
	
	MainScreenController controller = null;

	public static void main(String[] args)  {
		System.out.println("running");
		launch(args);

	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader fxmll = new FXMLLoader(getClass().getResource("MainScreen.fxml"));
		Parent root = fxmll.load();
		primaryStage.setScene(new Scene(root));
		primaryStage.show();
		controller = (MainScreenController)fxmll.getController();
		
	}
	
	@Override
	public void stop() {
		controller.stopSocket();
	}


}
