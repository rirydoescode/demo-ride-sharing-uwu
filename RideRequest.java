package com.example.demo;

import java.time.LocalDate;
import java.util.UUID;

public class RideRequest {

    private final String id;      // unique request ID
    private final String rideId;  // ride this request is for
    private final String seekerId;
    private final String seekerName;
    private String status;         // PENDING, ACCEPTED, REJECTED
    private final LocalDate requestDate; //  date

    /* ===================== CONSTRUCTORS ===================== */

    // New request
    public RideRequest(String seekerId, String seekerName, String rideId) {
        this.id = UUID.randomUUID().toString();
        this.rideId = rideId;
        this.seekerId = seekerId;
        this.seekerName = seekerName;
        this.status = "PENDING";
        this.requestDate = LocalDate.now();
    }

    //file loading
    public RideRequest(String id, String rideId, String seekerId, String seekerName, String status, LocalDate requestDate) {
        this.id = id;
        this.rideId = rideId;
        this.seekerId = seekerId;
        this.seekerName = seekerName;
        this.status = status.toUpperCase();
        this.requestDate = requestDate;
    }



    public String getId() { return id; }
    public String getRequestId() { return id; }
    public String getRideId() { return rideId; }
    public String getSeekerId() { return seekerId; }
    public String getSeekerName() { return seekerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status.toUpperCase(); }
    public LocalDate getRequestDate() { return requestDate; } // new getter



    @Override
    public String toString() {
        return String.join(",", id, rideId, seekerId, seekerName, status, requestDate.toString());
    }

    public static RideRequest fromString(String line) {
        String[] p = line.split(",");
        if (p.length != 6) return null; // updated length check
        return new RideRequest(
                p[0], // id
                p[1], // rideId
                p[2], // seekerId
                p[3], // seekerName
                p[4], // status
                LocalDate.parse(p[5]) // requestDate
        );
    }
}
