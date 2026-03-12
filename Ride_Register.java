package com.example.demo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ride_Register {

    private final String id;
    private final String providerID;
    private final String providerName;
    private final String providerGender; // NEW: provider gender
    private final String car;
    private final String destination;
    private final String departureTime;
    private final int totalSeats;
    private int bookedSeats;
    private final LocalDate datePosted;
    private double totalCost;

    private final List<String> pendingRequests = new ArrayList<>();

    /* ===================== CONSTRUCTORS ===================== */

    // New ride (provider creates)
    public Ride_Register(String providerID,
                         String providerName,
                         String providerGender, // added gender
                         String car,
                         String destination,
                         String departureTime,
                         int totalSeats,
                         double totalCost) {
        this(UUID.randomUUID().toString(), providerID, providerName, providerGender, car,
                destination, departureTime, totalSeats, 0, LocalDate.now(), totalCost);
    }

    // Load from file
    public Ride_Register(String id,
                         String providerID,
                         String providerName,
                         String providerGender,
                         String car,
                         String destination,
                         String departureTime,
                         int totalSeats,
                         int bookedSeats,
                         LocalDate datePosted,
                         double totalCost) {
        this.id = id;
        this.providerID = providerID;
        this.providerName = providerName;
        this.providerGender = providerGender;
        this.car = car;
        this.destination = destination;
        this.departureTime = departureTime;
        this.totalSeats = totalSeats;
        this.bookedSeats = bookedSeats;
        this.datePosted = datePosted;
        this.totalCost = totalCost;
    }
    private String providerId;

    public String getProviderId() {
        return providerId;
    }



    public String getId() { return id; }
    public String getProviderID() { return providerID; }
    public String getProviderName() { return providerName; }
    public String getProviderGender() { return providerGender; } // new getter
    public String getCar() { return car; }
    public String getDestination() { return destination; }
    public String getDepartureTime() { return departureTime; }
    public int getTotalSeats() { return totalSeats; }
    public int getBookedSeats() { return bookedSeats; }
    public int getAvailableSeats() { return totalSeats - bookedSeats; }
    public List<String> getPendingRequests() { return pendingRequests; }
    public LocalDate getDatePosted() { return datePosted; }
    public double getTotalCost() { return totalCost; }
    public double getCostPerSeat() { return totalSeats > 0 ? totalCost / totalSeats : 0; }



    public boolean bookSeat() {
        if (bookedSeats >= totalSeats) return false;
        bookedSeats++;
        return true;
    }

    public boolean addRequest(String seekerId) {
        if (!pendingRequests.contains(seekerId)) {
            pendingRequests.add(seekerId);
            return true;
        }
        return false;
    }

    public boolean removeRequest(String seekerId) {
        return pendingRequests.remove(seekerId);
    }



    @Override
    public String toString() {
        return String.join(",",
                id,
                providerID,
                providerName,
                providerGender, // added gender
                car,
                destination,
                departureTime,
                String.valueOf(totalSeats),
                String.valueOf(bookedSeats),
                datePosted.toString(),
                String.valueOf(totalCost)
        );
    }

    public static Ride_Register fromString(String line) {
        try {
            String[] p = line.split(",");
            return new Ride_Register(
                    p[0],               // id
                    p[1],               // providerID
                    p[2],               // providerName
                    p[3],               // providerGender
                    p[4],               // car
                    p[5],               // destination
                    p[6],               // departureTime
                    Integer.parseInt(p[7]), // totalSeats
                    Integer.parseInt(p[8]), // bookedSeats
                    LocalDate.parse(p[9]), // datePosted
                    Double.parseDouble(p[10]) // totalCost
            );
        } catch (Exception e) {
            return null; // corrupted line ignores
        }
    }

    //show it

    public String display() {
        return providerName + " (" + providerGender + ")" + " | " + destination +
                " | " + departureTime +
                " | Seats left: " + getAvailableSeats() +
                " | Posted: " + datePosted +
                " | Cost per seat: $" + String.format("%.2f", getCostPerSeat());
    }

    public String displayWithRequests() {
        return display() + " | Pending Requests: " + pendingRequests.size();
    }
}
