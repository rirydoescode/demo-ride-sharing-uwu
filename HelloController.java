package com.example.demo;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HelloController {

    @FXML
    private TextField universityIdField;

    @FXML
    private PasswordField passwordField;

    private LoginManager manager = new LoginManager();

    // pass
    @FXML
    private void handleRegister() {
        String id = universityIdField.getText();
        String pw = passwordField.getText();

        if (id.isEmpty() || pw.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Registration", "Please enter both ID and password!");
            return;
        }

        if (!manager.idExists(id)) {
            showAlert(Alert.AlertType.ERROR, "Registration", "Invalid University ID!");
            return;
        }

        if (manager.setPassword(id, pw)) {
            showAlert(Alert.AlertType.INFORMATION, "Registration", "Password set successfully! You can now login.");
            passwordField.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Registration", "Password could not be set. Maybe it was already set?");
        }
    }

    // login stuff
    @FXML
    private void handleLogin() {
        String id = universityIdField.getText();
        String pw = passwordField.getText();

        if (id.isEmpty() || pw.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login", "Please enter both ID and password!");
            return;
        }

        if (!manager.idExists(id)) {
            showAlert(Alert.AlertType.ERROR, "Login", "Invalid University ID!");
            return;
        }

        if (!manager.validateLogin(id, pw)) {
            showAlert(Alert.AlertType.ERROR, "Login", "Incorrect password!");
            return;
        }

        // ---------- ROLE SELECTION ----------
        List<String> roles = Arrays.asList("Seeker", "Provider");
        ChoiceDialog<String> roleDialog = new ChoiceDialog<>("Seeker", roles);
        roleDialog.setTitle("Select Role");
        roleDialog.setHeaderText("Choose your role for this session");
        roleDialog.setContentText("Role:");

        Optional<String> result = roleDialog.showAndWait();

        if (result.isEmpty()) return;

        String selectedRole = result.get().toLowerCase();

        try {
            FXMLLoader fxmlLoader;
            Stage stage = new Stage();

            if ("seeker".equals(selectedRole)) {
                fxmlLoader = new FXMLLoader(
                        getClass().getResource("/com/example/demo/ride_seeker.fxml")
                );
                Scene scene = new Scene(fxmlLoader.load());

                RideSeekerController controller = fxmlLoader.getController();
                controller.setSeekerInfo(id, manager.getUserName(id));

                stage.setTitle("Campus Ride Seeker");
                stage.setScene(scene);
            } else {
                fxmlLoader = new FXMLLoader(
                        getClass().getResource("/com/example/demo/ride_provider.fxml")
                );
                Scene scene = new Scene(fxmlLoader.load());

                RideProviderController controller = fxmlLoader.getController();
                controller.setProviderInfo(id, manager.getUserName(id));

                stage.setTitle("Campus Ride Provider");
                stage.setScene(scene);
            }

            stage.show();

            // close it
            ((Stage) universityIdField.getScene().getWindow()).close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Helper method
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
