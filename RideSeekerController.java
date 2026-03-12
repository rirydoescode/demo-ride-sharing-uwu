package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RideSeekerController {

    @FXML private TextField destinationField;
    @FXML private TextField departureField;
    @FXML private ChoiceBox<String> genderFilterChoice;
    @FXML private ListView<String> ridesList;
    @FXML private TextField rideIdField;
    @FXML private TextField totalCostField;
    @FXML private TextField costPerSeatField;
    @FXML private ListView<String> myRequestsList;
    @FXML private AnchorPane mapPane;
    @FXML private ImageView mapView;
    @FXML private ChoiceBox<String> pointChoice; // select point for route
    @FXML private Button chatButton; // Chat button

    private final Ride_Manager rideManager = new Ride_Manager();
    private String seekerId;
    private String seekerName;

    // Original image size
    private final double originalWidth = 1254;
    private final double originalHeight = 785;

    // og pic
    private final int campusX = 661;
    private final int campusY = 194;

    // points
    private final Map<String, int[]> pointsMap = new HashMap<>();

    // Chat file
    private final String chatFile = "src/main/resources/chats.txt";

    @FXML
    private void initialize() {
        genderFilterChoice.getItems().addAll("Any", "Male", "Female");
        genderFilterChoice.setValue("Any");

        pointsMap.put("A", new int[]{800, 100});
        pointsMap.put("B", new int[]{900, 250});
        pointsMap.put("C", new int[]{700, 300});
        pointsMap.put("D", new int[]{850, 400});
        pointChoice.getItems().addAll("A", "B", "C", "D");
        pointChoice.setValue("A");

        ridesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() && !newVal.equals("No rides found.")) {
                String rideId = newVal.split("\\|")[0].replace("Ride ID:", "").trim();
                rideIdField.setText(rideId);
                Ride_Register ride = rideManager.getRideById(rideId);
                if (ride != null) {
                    totalCostField.setText(String.format("%.2f", ride.getTotalCost()));
                    costPerSeatField.setText(String.format("%.2f", ride.getCostPerSeat()));
                } else {
                    totalCostField.clear();
                    costPerSeatField.clear();
                }
            }
        });

        // Zoom
        mapPane.setOnScroll(event -> {
            double scale = mapPane.getScaleX();
            double delta = event.getDeltaY() > 0 ? 1.1 : 0.9;
            mapPane.setScaleX(scale * delta);
            mapPane.setScaleY(scale * delta);
            event.consume();
        });

        // Chat button action
        chatButton.setOnAction(e -> {
            String rideId = rideIdField.getText().trim();
            if (rideId.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Chat Error", "Please select a ride first.");
                return;
            }
            Ride_Register ride = rideManager.getRideById(rideId);
            if (ride != null) {
                openChat(rideId, ride.getProviderID());
            } else {
                showAlert(Alert.AlertType.ERROR, "Chat Error", "Ride not found.");
            }
        });
    }

    public void setSeekerInfo(String id, String name) {
        this.seekerId = id;
        this.seekerName = name;
        loadMyRequests();
    }

    @FXML
    private void handleSearch() {
        rideManager.loadRidesFromFile();
        String destination = destinationField.getText().trim();
        String departure = departureField.getText().trim();
        String genderFilter = genderFilterChoice.getValue();

        List<Ride_Register> results = rideManager.search(destination, departure);
        if (!"Any".equalsIgnoreCase(genderFilter)) {
            results = results.stream()
                    .filter(r -> r.getProviderGender().equalsIgnoreCase(genderFilter))
                    .collect(Collectors.toList());
        }
        results.sort((r1, r2) -> r2.getDatePosted().compareTo(r1.getDatePosted()));

        ridesList.getItems().clear();
        if (results.isEmpty()) {
            ridesList.getItems().add("No rides found.");
            return;
        }
        for (Ride_Register r : results) {
            String rideInfo = String.format(
                    "Ride ID: %s | Provider: %s | Gender: %s | Destination: %s | Departure: %s | Seats left: %d | Total cost: $%.2f | Cost per seat: $%.2f | Posted: %s",
                    r.getId(),
                    r.getProviderName(),
                    r.getProviderGender(),
                    r.getDestination(),
                    r.getDepartureTime(),
                    r.getAvailableSeats(),
                    r.getTotalCost(),
                    r.getCostPerSeat(),
                    r.getDatePosted()
            );
            ridesList.getItems().add(rideInfo);
        }
    }

    @FXML
    private void handleRequest() {
        if (seekerId == null || seekerName == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "Seeker information not loaded. Please log in again.");
            return;
        }
        String rideId = rideIdField.getText().trim();
        if (rideId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Request Error", "Please select a ride first.");
            return;
        }
        boolean success = rideManager.sendRequest(seekerId, seekerName, rideId);
        showAlert(
                success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "Request Status",
                success ? "Request sent! Wait for provider to accept." : "Failed to send request (ride not found)."
        );
        loadMyRequests();
    }

    @FXML
    private void loadMyRequests() {
        rideManager.loadRequestsFromFile();
        myRequestsList.getItems().clear();
        List<RideRequest> myRequests = rideManager.getRequestsForSeeker(seekerId);
        if (myRequests.isEmpty()) {
            myRequestsList.getItems().add("No requests yet.");
            return;
        }
        myRequests.sort((r1, r2) -> r2.getRequestDate().compareTo(r1.getRequestDate()));
        for (RideRequest req : myRequests) {
            Ride_Register ride = rideManager.getRideById(req.getRideId());
            String rideInfo = String.format(
                    "Request Date: %s | Ride: %s | Departure: %s | Seats left: %s | Total cost: $%s | Cost per seat: $%s | Provider Gender: %s | Status: %s",
                    req.getRequestDate(),
                    ride != null ? ride.getDestination() : "Unknown",
                    ride != null ? ride.getDepartureTime() : "N/A",
                    ride != null ? ride.getAvailableSeats() : "N/A",
                    ride != null ? String.format("%.2f", ride.getTotalCost()) : "N/A",
                    ride != null ? String.format("%.2f", ride.getCostPerSeat()) : "N/A",
                    ride != null ? ride.getProviderGender() : "N/A",
                    req.getStatus()
            );
            myRequestsList.getItems().add(rideInfo);
        }
    }

    @FXML
    private void handleViewMap() {
        if (mapView.getImage() == null) {
            Image mapImage = new Image(getClass().getResourceAsStream("/maps/campus_map.png"));
            mapView.setImage(mapImage);
        }
        mapView.setVisible(!mapView.isVisible());
        if (mapView.isVisible()) drawMapRoute();
    }

    @FXML
    private void drawMapRoute() {
        mapPane.getChildren().removeIf(node -> node instanceof Circle || node instanceof Line);
        double scaleX = mapView.getBoundsInParent().getWidth() / originalWidth;
        double scaleY = mapView.getBoundsInParent().getHeight() / originalHeight;

        double cx = campusX * scaleX;
        double cy = campusY * scaleY;
        Circle campus = new Circle(cx, cy, 8, Color.RED);
        mapPane.getChildren().add(campus);

        String selected = pointChoice.getValue();
        if (selected == null || !pointsMap.containsKey(selected)) return;
        int[] target = pointsMap.get(selected);
        double tx = target[0] * scaleX;
        double ty = target[1] * scaleY;

        Circle targetPoint = new Circle(tx, ty, 6, Color.BLUE);
        mapPane.getChildren().add(targetPoint);

        Line line1 = new Line(cx, cy, (cx + tx) / 2, cy);
        line1.setStroke(Color.ORANGE);
        line1.setStrokeWidth(3);
        Line line2 = new Line((cx + tx) / 2, cy, (cx + tx) / 2, ty);
        line2.setStroke(Color.ORANGE);
        line2.setStrokeWidth(3);
        Line line3 = new Line((cx + tx) / 2, ty, tx, ty);
        line3.setStroke(Color.ORANGE);
        line3.setStrokeWidth(3);

        mapPane.getChildren().addAll(line1, line2, line3);
    }

    //chat stuff

    private void openChat(String rideId, String providerId) {
        String chatId = seekerId + "_" + providerId + "_" + rideId;

        Stage chatStage = new Stage();
        chatStage.initModality(Modality.APPLICATION_MODAL);
        chatStage.setTitle("Chat");

        VBox root = new VBox(5);
        root.setPrefSize(400, 400);

        ListView<String> chatList = new ListView<>();
        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");
        Button sendButton = new Button("Send");
        Button deleteButton = new Button("Delete Chat");

        HBox inputBox = new HBox(5, inputField, sendButton);
        root.getChildren().addAll(chatList, inputBox, deleteButton);

        loadChat(chatId, chatList);

        sendButton.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                sendMessage(chatId, "seeker", text);
                chatList.getItems().add("[You] " + text);
                inputField.clear();
            }
        });

        deleteButton.setOnAction(e -> {
            deleteChat(chatId);
            chatList.getItems().clear();
            showAlert(Alert.AlertType.INFORMATION, "Chat Deleted", "Chat has been deleted successfully.");
        });

        Scene scene = new Scene(root);
        chatStage.setScene(scene);
        chatStage.show();
    }

    private void loadChat(String chatId, ListView<String> chatList) {
        chatList.getItems().clear();
        try {
            if (!Files.exists(Paths.get(chatFile))) return;

            List<String> lines = Files.readAllLines(Paths.get(chatFile));
            boolean inChat = false;
            for (String line : lines) {
                if (line.startsWith("chatId:")) {
                    inChat = line.substring(7).trim().equals(chatId);
                } else if (inChat && !line.trim().isEmpty()) {
                    chatList.getItems().add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String chatId, String sender, String text) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String messageLine = String.format("[%s] %s: %s", timeStamp, sender, text);

        try {
            List<String> lines = Files.exists(Paths.get(chatFile)) ? Files.readAllLines(Paths.get(chatFile)) : new ArrayList<>();
            boolean chatExists = lines.stream().anyMatch(l -> l.equals("chatId: " + chatId));

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatFile, true))) {
                if (!chatExists) {
                    writer.write("chatId: " + chatId);
                    writer.newLine();
                }
                writer.write(messageLine);
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteChat(String chatId) {
        try {
            if (!Files.exists(Paths.get(chatFile))) return;
            List<String> lines = Files.readAllLines(Paths.get(chatFile));
            List<String> newLines = new ArrayList<>();
            boolean inChat = false;

            for (String line : lines) {
                if (line.startsWith("chatId:")) {
                    inChat = line.substring(7).trim().equals(chatId);
                    if (!inChat) newLines.add(line);
                } else if (!inChat) {
                    newLines.add(line);
                }
            }

            Files.write(Paths.get(chatFile), newLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //chat ends

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
