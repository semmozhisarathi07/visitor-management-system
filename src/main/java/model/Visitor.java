package model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Visitor.java
 *
 * Represents ONE visitor record in the application.
 * This is a "model" class: it only holds data (fields + getters/setters).
 * It does not talk to the database and it does not build the UI.
 *
 * The "totalDuration" is stored as MINUTES (for example 75 means 1 hr 15 min).
 */
public class Visitor {

    private int visitorId;          // generated automatically by MySQL
    private String visitorName;
    private String phone;
    private String personToMeet;
    private String reason;
    private LocalDate visitDate;    // e.g. 2026-10-05
    private LocalTime inTime;       // e.g. 10:30
    private LocalTime outTime;      // null while the visitor is still INSIDE
    private Integer totalDuration;  // minutes, null while the visitor is still INSIDE
    private String status;          // "INSIDE" or "OUT"

    public Visitor() {
    }

    // ---------- Getters ----------

    public int getVisitorId() {
        return visitorId;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPersonToMeet() {
        return personToMeet;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public LocalTime getInTime() {
        return inTime;
    }

    public LocalTime getOutTime() {
        return outTime;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public String getStatus() {
        return status;
    }

    // ---------- Setters ----------

    public void setVisitorId(int visitorId) {
        this.visitorId = visitorId;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPersonToMeet(String personToMeet) {
        this.personToMeet = personToMeet;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public void setInTime(LocalTime inTime) {
        this.inTime = inTime;
    }

    public void setOutTime(LocalTime outTime) {
        this.outTime = outTime;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ---------- Helper methods ----------

    /**
     * Formats the duration for display.
     *   75 minutes -> "1 hr 15 min"
     *   30 minutes -> "30 min"
     * Returns "-" when the visitor has not checked out yet.
     */
    public static String formatDuration(Integer minutes) {
        if (minutes == null) {
            return "-";
        }
        long h = minutes / 60;
        long m = minutes % 60;
        if (h == 0) {
            return m + " min";
        }
        return h + " hr " + m + " min";
    }

    @Override
    public String toString() {
        return "Visitor{" +
                "id=" + visitorId +
                ", name='" + visitorName + '\'' +
                ", phone='" + phone + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
