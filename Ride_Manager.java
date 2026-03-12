package com.example.demo;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Ride_Manager {

    private final List<Ride_Register> rides = new ArrayList<>();
    private final List<RideRequest> requests = new ArrayList<>();

    private final File ridesFile = new File("rides.txt");
    private final File requestsFile = new File("requests.txt");

    public Ride_Manager() {
        loadRidesFromFile();
        loadRequestsFromFile();
    }

    //file

    public void loadRidesFromFile() {
        rides.clear();
        if (!ridesFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(ridesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                Ride_Register ride = Ride_Register.fromString(line);
                if (ride != null) rides.add(ride);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAllRidesToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ridesFile))) {
            for (Ride_Register r : rides) {
                bw.write(r.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadRequestsFromFile() {
        requests.clear();
        if (!requestsFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(requestsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                RideRequest req = RideRequest.fromString(line);
                if (req != null) requests.add(req);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAllRequestsToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(requestsFile))) {
            for (RideRequest req : requests) {
                bw.write(req.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //provider part

    public void addRide(Ride_Register ride) {
        rides.add(ride);
        saveAllRidesToFile();
    }

    public List<RideRequest> getRequestsForProvider(String providerId) {
        loadRequestsFromFile();
        loadRidesFromFile();

        List<RideRequest> providerRequests = new ArrayList<>();
        for (RideRequest req : requests) {
            Ride_Register ride = getRideById(req.getRideId());
            if (ride != null
                    && ride.getProviderID().equals(providerId)
                    && req.getStatus().equalsIgnoreCase("PENDING")) {
                providerRequests.add(req);
            }
        }

        providerRequests.sort((r1, r2) -> r2.getRequestDate().compareTo(r1.getRequestDate()));
        return providerRequests;
    }

    public boolean respondToRequest(String requestId, boolean accept) {
        loadRequestsFromFile();
        loadRidesFromFile();

        for (RideRequest req : requests) {
            if (req.getId().equals(requestId)
                    && req.getStatus().equalsIgnoreCase("PENDING")) {

                if (accept) {
                    Ride_Register ride = getRideById(req.getRideId());
                    if (ride != null && ride.getAvailableSeats() > 0) {
                        ride.bookSeat();
                        req.setStatus("ACCEPTED");
                        saveAllRidesToFile();
                    } else {
                        return false;
                    }
                } else {
                    req.setStatus("REJECTED");
                }

                saveAllRequestsToFile();
                return true;
            }
        }
        return false;
    }

    public Ride_Register getRideById(String rideId) {
        for (Ride_Register r : rides) {
            if (r.getId().equals(rideId)) return r;
        }
        return null;
    }

    public List<Ride_Register> getRidesForProvider(String providerId) {
        loadRidesFromFile();
        return rides.stream()
                .filter(r -> r.getProviderID().equals(providerId))
                .collect(Collectors.toList());
    }

    //seeker part

    // Get seeker name by their ID
    public String getSeekerNameById(String seekerId) {
        loadRequestsFromFile();
        for (RideRequest req : requests) {
            if (req.getSeekerId().equals(seekerId)) {
                return req.getSeekerName();
            }
        }
        return seekerId;
    }

    // Get seeker ID by name (for provider chat)
    public String getSeekerIdByName(String seekerName) {
        loadRequestsFromFile();
        for (RideRequest req : requests) {
            if (req.getSeekerName().equalsIgnoreCase(seekerName)) {
                return req.getSeekerId();
            }
        }
        return null;
    }

    //seeker
    public List<String> getAllSeekersForProvider(String providerId){
        loadRequestsFromFile();
        loadRidesFromFile();

        Set<String> seekers = new HashSet<>();
        for(RideRequest req : requests){
            Ride_Register ride = getRideById(req.getRideId());
            if(ride != null && ride.getProviderID().equals(providerId)){

                seekers.add(req.getSeekerName() + " | " + req.getRideId() + " | " + req.getSeekerId());
            }
        }

        return new ArrayList<>(seekers);
    }

    public List<Ride_Register> search(String destination, String departure) {
        loadRidesFromFile();

        return rides.stream()
                .filter(r -> (destination == null || destination.isBlank() ||
                        r.getDestination().toLowerCase().contains(destination.toLowerCase()))
                        && (departure == null || departure.isBlank() ||
                        r.getDepartureTime().toLowerCase().contains(departure.toLowerCase()))
                        && r.getAvailableSeats() > 0)
                .sorted((a, b) -> b.getDatePosted().compareTo(a.getDatePosted()))
                .collect(Collectors.toList());
    }

    public boolean sendRequest(String seekerId, String seekerName, String rideId) {
        loadRequestsFromFile();
        Ride_Register ride = getRideById(rideId);
        if (ride == null) return false;

        RideRequest req = new RideRequest(seekerId, seekerName, rideId);
        requests.add(req);
        saveAllRequestsToFile();
        return true;
    }

    public List<RideRequest> getRequestsForSeeker(String seekerId) {
        loadRequestsFromFile();
        return requests.stream()
                .filter(r -> r.getSeekerId().equals(seekerId))
                .sorted((r1, r2) -> r2.getRequestDate().compareTo(r1.getRequestDate()))
                .collect(Collectors.toList());
    }
}
