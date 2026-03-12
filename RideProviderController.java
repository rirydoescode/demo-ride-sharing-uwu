package com.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RideProviderController {

    @FXML private TextField carField, destinationField, departureField, seatsField, totalCostField;
    @FXML private ChoiceBox<String> genderChoice, pointChoice, chatSelector;
    @FXML private ListView<String> requestsList, myRidesList;
    @FXML private Button chatButton;
    @FXML private AnchorPane mapPane;
    @FXML private ImageView mapView;

    private final Ride_Manager rideManager = new Ride_Manager();
    private String providerID;
    private String providerName;

    private final int campusX = 661;
    private final int campusY = 194;
    private final Map<String, int[]> pointsMap = new HashMap<>();

    @FXML
    private void initialize() {
        genderChoice.getItems().addAll("Male", "Female");
        genderChoice.setValue("Male");

        pointsMap.put("A", new int[]{800, 100});
        pointsMap.put("B", new int[]{900, 250});
        pointsMap.put("C", new int[]{700, 300});
        pointsMap.put("D", new int[]{850, 400});

        pointChoice.getItems().addAll("A", "B", "C", "D");
        pointChoice.setValue("A");

        pointChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> drawMapRoute());

        mapPane.setOnScroll(event -> {
            double scale = mapPane.getScaleX();
            double delta = event.getDeltaY() > 0 ? 1.1 : 0.9;
            mapPane.setScaleX(scale * delta);
            mapPane.setScaleY(scale * delta);
            event.consume();
        });

        chatButton.setOnAction(e -> openChatWindow());
    }

    public void setProviderInfo(String id, String name) {
        this.providerID = id;
        this.providerName = name;
        loadMyRides();
        loadRequests();
        loadChatSelector(); // initial load
    }

    @FXML
    private void handleAddRide() {
        if (providerID == null || providerName == null) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "Provider info not loaded.");
            return;
        }

        String car = carField.getText().trim();
        String dest = destinationField.getText().trim();
        String depart = departureField.getText().trim();
        String seatsText = seatsField.getText().trim();
        String costText = totalCostField.getText().trim();
        String gender = genderChoice.getValue();

        if (car.isEmpty() || dest.isEmpty() || depart.isEmpty() || seatsText.isEmpty() || costText.isEmpty() || gender == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Info", "All fields are required.");
            return;
        }

        int seats;
        double totalCost;
        try { seats = Integer.parseInt(seatsText); if(seats <= 0) throw new NumberFormatException(); }
        catch(Exception e){ showAlert(Alert.AlertType.ERROR,"Invalid Seats","Seats must be positive."); return; }
        try { totalCost = Double.parseDouble(costText); if(totalCost < 0) throw new NumberFormatException(); }
        catch(Exception e){ showAlert(Alert.AlertType.ERROR,"Invalid Cost","Cost must be non-negative."); return; }

        Ride_Register ride = new Ride_Register(providerID, providerName, gender, car, dest, depart, seats, totalCost);
        rideManager.addRide(ride);
        showAlert(Alert.AlertType.INFORMATION,"Ride Added","Your ride has been posted!");
        clearFields();
        loadMyRides();
    }

    @FXML private void loadRequests() {
        if(providerID == null) return;

        List<RideRequest> providerRequests = rideManager.getRequestsForProvider(providerID);
        requestsList.getItems().clear();
        if(providerRequests.isEmpty()) requestsList.getItems().add("No pending requests.");
        else {
            for(RideRequest req : providerRequests){
                Ride_Register ride = rideManager.getRideById(req.getRideId());
                requestsList.getItems().add(req.getId() + " | Ride: " + ride.getDestination() + " | Seeker: " + req.getSeekerName() + " | Status: " + req.getStatus());
            }
        }
    }

    private void handleRespond(boolean accept){
        String selected = requestsList.getSelectionModel().getSelectedItem();
        if(selected == null || selected.equals("No pending requests.")) {
            showAlert(Alert.AlertType.WARNING,"No Selection","Select a request.");
            return;
        }

        String requestId = selected.split("\\|")[0].trim();
        boolean success = rideManager.respondToRequest(requestId, accept);
        showAlert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "Response Status", success ? "Response sent!" : "Failed to respond.");
        loadRequests();
        loadChatSelector(); //select the thing
    }

    @FXML private void handleAccept(){ handleRespond(true); }
    @FXML private void handleReject(){ handleRespond(false); }

    @FXML private void loadMyRides(){
        if(providerID == null) return;
        rideManager.loadRidesFromFile();
        myRidesList.getItems().clear();
        for(Ride_Register ride: rideManager.getRidesForProvider(providerID)){
            myRidesList.getItems().add(
                    ride.getId() + " | " + ride.getDestination() + " | Departure: " + ride.getDepartureTime() +
                            " | Seats: " + ride.getAvailableSeats() + "/" + ride.getTotalSeats() +
                            " | Posted: " + ride.getDatePosted() +
                            " | Gender: " + ride.getProviderGender() +
                            " | Cost per seat: $" + String.format("%.2f", ride.getCostPerSeat())
            );
        }
    }

    @FXML
    private void handleViewMap(){
        if(mapView.getImage() == null){
            Image mapImage = new Image(getClass().getResourceAsStream("/maps/campus_map.png"));
            mapView.setImage(mapImage);
        }
        mapView.setVisible(!mapView.isVisible());
        if(mapView.isVisible()) drawMapRoute();
    }

    @FXML
    private void drawMapRoute(){
        if(mapView.getImage() == null) return;
        mapPane.getChildren().removeIf(node -> node instanceof Circle || node instanceof Line);

        double scaleX = mapView.getBoundsInParent().getWidth() / mapView.getImage().getWidth();
        double scaleY = mapView.getBoundsInParent().getHeight() / mapView.getImage().getHeight();

        double scaledCampusX = campusX*scaleX;
        double scaledCampusY = campusY*scaleY;
        Circle campus = new Circle(scaledCampusX, scaledCampusY, 8, Color.RED);
        mapPane.getChildren().add(campus);

        String selected = pointChoice.getValue();
        if(selected==null || !pointsMap.containsKey(selected)) return;

        int[] target = pointsMap.get(selected);
        double targetX = target[0]*scaleX;
        double targetY = target[1]*scaleY;

        Circle targetPoint = new Circle(targetX, targetY, 6, Color.BLUE);
        mapPane.getChildren().add(targetPoint);

        Line line1 = new Line(scaledCampusX, scaledCampusY, (scaledCampusX+targetX)/2, scaledCampusY);
        Line line2 = new Line((scaledCampusX+targetX)/2, scaledCampusY, (scaledCampusX+targetX)/2, targetY);
        Line line3 = new Line((scaledCampusX+targetX)/2, targetY, targetX, targetY);

        line1.setStroke(Color.ORANGE); line2.setStroke(Color.ORANGE); line3.setStroke(Color.ORANGE);
        line1.setStrokeWidth(3); line2.setStrokeWidth(3); line3.setStrokeWidth(3);

        mapPane.getChildren().addAll(line1,line2,line3);
    }

    private void clearFields(){
        carField.clear(); destinationField.clear(); departureField.clear();
        seatsField.clear(); totalCostField.clear();
        genderChoice.setValue("Male");
    }

    private void showAlert(Alert.AlertType type, String title, String message){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //chat
    private void loadChatSelector(){
        if(providerID==null) return;

        String currentSelection = chatSelector.getValue(); // remember previous selection
        chatSelector.getItems().clear();
        chatSelector.getItems().add("Select a seeker...");

        List<String> seekers = rideManager.getAllSeekersForProvider(providerID);
        chatSelector.getItems().addAll(seekers);

        // restore previous selection if possible
        if(currentSelection != null && chatSelector.getItems().contains(currentSelection)){
            chatSelector.setValue(currentSelection);
        } else {
            chatSelector.getSelectionModel().selectFirst();
        }
    }


    private void openChatWindow(){
        String selected = chatSelector.getValue();
        if(selected==null || selected.equals("Select a seeker...")){
            showAlert(Alert.AlertType.WARNING,"No Selection","Select a seeker to chat.");
            return;
        }

        String[] parts = selected.split("\\|");
        String seekerName = parts[0].trim();
        String rideId = parts[1].trim();
        String seekerId = parts[2].trim();

        Stage chatStage = new Stage();
        ChatBox chatBox = new ChatBox(seekerId, providerID, rideId);
        chatStage.setTitle("Chat with "+seekerName);
        chatStage.setScene(new Scene(chatBox,400,300));
        chatStage.show();
    }

    public static class ChatBox extends VBox {
        private final String seekerId, providerId, rideId;
        private TextArea chatArea;
        private TextField inputField;
        private Button sendButton;
        private final String chatFilePath = "src/main/resources/chats.txt";

        public ChatBox(String seekerId, String providerId, String rideId){
            this.seekerId = seekerId;
            this.providerId = providerId;
            this.rideId = rideId;

            this.setSpacing(5);
            this.setStyle("-fx-padding:10;");

            chatArea = new TextArea();
            chatArea.setEditable(false);
            chatArea.setWrapText(true);

            inputField = new TextField();
            inputField.setPromptText("Type a message...");

            sendButton = new Button("Send");

            HBox controls = new HBox(5,inputField,sendButton);
            this.getChildren().addAll(chatArea,controls);

            loadMessages();

            sendButton.setOnAction(e -> sendMessage());

            // auto refresh to see all chats
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> loadMessages()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }

        private void loadMessages(){
            chatArea.clear();
            File file = new File(chatFilePath);
            if(!file.exists()) return;
            try(BufferedReader br = new BufferedReader(new FileReader(file))){
                String line;
                boolean readingThisChat=false;
                while((line=br.readLine())!=null){
                    if(line.startsWith("chatId:")){
                        readingThisChat = line.contains(seekerId+"_"+providerId+"_"+rideId);
                        continue;
                    }
                    if(readingThisChat){
                        chatArea.appendText(line+"\n");
                    }
                }
            } catch(IOException ignored){}
        }

        private void sendMessage(){
            String msg = inputField.getText().trim();
            if(msg.isEmpty()) return;
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String line = "["+timestamp+"] provider: "+msg;

            appendToFile(line);

            chatArea.appendText(line+"\n");
            inputField.clear();
        }

        private void appendToFile(String line){
            File file = new File(chatFilePath);
            try{
                List<String> allLines = file.exists() ? new ArrayList<>(java.nio.file.Files.readAllLines(file.toPath())) : new ArrayList<>();
                boolean chatExists=false;

                for(String l: allLines){
                    if(l.startsWith("chatId:") && l.contains(seekerId+"_"+providerId+"_"+rideId)){
                        chatExists=true;
                        break;
                    }
                }

                BufferedWriter bw = new BufferedWriter(new FileWriter(file,true));
                if(!chatExists){
                    bw.write("chatId: "+seekerId+"_"+providerId+"_"+rideId);
                    bw.newLine();
                }
                bw.write(line);
                bw.newLine();
                bw.close();
            } catch(IOException ignored){}
        }
    }
}
