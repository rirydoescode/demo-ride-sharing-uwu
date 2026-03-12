//this is a redundant file. i dont need this. it's there for backup




package com.example.demo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RideRequestManager {

    private final List<RideRequest> requests = new ArrayList<>();
    private final File file = new File("requests.txt");

    public RideRequestManager() {
        loadRequestsFromFile();
    }

    public void loadRequestsFromFile() {
        requests.clear();
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                RideRequest r = RideRequest.fromString(line);
                if (r != null) requests.add(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAllRequestsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (RideRequest r : requests) {
                bw.write(r.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRequest(RideRequest r) {
        requests.add(r);
        saveAllRequestsToFile();
    }

    public List<RideRequest> getRequestsForProvider(String providerId, Ride_Manager rideManager) {
        loadRequestsFromFile();
        List<RideRequest> result = new ArrayList<>();
        for (RideRequest r : requests) {
            Ride_Register ride = rideManager.search("", "").stream()
                    .filter(x -> x.getId().equals(r.getRideId()) && x.getProviderID().equals(providerId))
                    .findFirst().orElse(null);
            if (ride != null && r.getStatus().equals("PENDING")) result.add(r);
        }
        return result;
    }

    public boolean acceptRequest(String requestId, Ride_Manager rideManager) {
        loadRequestsFromFile();
        for (RideRequest r : requests) {
            if (r.getRequestId().equals(requestId) && r.getStatus().equals("PENDING")) {
                Ride_Register ride = rideManager.search("", "").stream()
                        .filter(x -> x.getId().equals(r.getRideId()))
                        .findFirst().orElse(null);
                if (ride != null && ride.bookSeat()) {
                    r.setStatus("ACCEPTED");
                    saveAllRequestsToFile();
                    rideManager.addRide(ride); // update ride seats
                    return true;
                } else {
                    r.setStatus("REJECTED");
                    saveAllRequestsToFile();
                    return false;
                }
            }
        }
        return false;
    }

    public boolean rejectRequest(String requestId) {
        loadRequestsFromFile();
        for (RideRequest r : requests) {
            if (r.getRequestId().equals(requestId) && r.getStatus().equals("PENDING")) {
                r.setStatus("REJECTED");
                saveAllRequestsToFile();
                return true;
            }
        }
        return false;
    }
}
