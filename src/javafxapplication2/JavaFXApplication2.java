/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javafxapplication2;

import java.util.List;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Core;

/**
 *
 * @author Keenan
 */
public class JavaFXApplication2 extends Application {
    
    /*@Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
    }*/
    
    @Override
     public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLDocument.fxml"));

        // Create a controller instance
        List<String> params = getParameters().getRaw();
        System.out.println(params);
        FXHelloCVController controller = new FXHelloCVController(params.get(0), params.get(1));
        // Set it in the FXMLLoader
        loader.setController(controller);
        BorderPane borderPane = loader.load();
        Scene scene = new Scene(borderPane, 200, 200);
        primaryStage.setTitle(params.get(1));
        primaryStage.setScene(scene);
        primaryStage.show();
     }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }
    
}
