package service;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.VisitorDAO;
import model.Visitor;

/**
 * VisitorService.java
 *
 * Contains all BUSINESS LOGIC of the application:
 *   - validating user input
 *   - filling in the automatic values (ID, date, in time, status)
 *   - checking visitors out and calculating the duration
 *   - turning SQL errors into friendly messages
 *
 * The UI (ui package) only talks to this class,
 * never directly to the database.
 */
public class VisitorService {

    private final VisitorDAO visitorDAO = new VisitorDAO();

    // =========================================================
    // Register
    // =========================================================

    /**
     * Registers a new visitor.
     * Visit date, in time and status are set automatically.
     *
     * @return the saved visitor, including the generated visitor ID.
     */
    public Visitor registerVisitor(String name, String phone, String personToMeet, String reason) {
        validateName(name);
        String cleanPhone = validatePhone(phone);
        if (personToMeet == null || personToMeet.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter the person to meet.");
        }

        Visitor v = new Visitor();
        v.setVisitorName(name.trim());
        v.setPhone(cleanPhone);
        v.setPersonToMeet(personToMeet.trim());
        v.setReason(reason == null ? "" : reason.trim());
        v.setVisitDate(LocalDate.now());   // automatic
        v.setInTime(LocalTime.now());      // automatic
        v.setStatus("INSIDE");             // automatic

        try {
            int generatedId = visitorDAO.insertVisitor(v);
            v.setVisitorId(generatedId);
            return v;
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    // =========================================================
    // Check out
    // =========================================================

    /**
     * Checks a visitor out:
     *   1. records the current out time
     *   2. calculates the total duration in minutes
     *   3. changes the status from INSIDE to OUT
     *
     * @return the updated visitor record (to show to the user).
     */
    public Visitor checkOutVisitor(int visitorId) {
        try {
            Visitor v = visitorDAO.findById(visitorId);
            if (v == null) {
                throw new IllegalArgumentException("Visitor not found. Check the Visitor ID.");
            }
            if (!"INSIDE".equalsIgnoreCase(v.getStatus())) {
                throw new IllegalStateException("Visitor " + v.getVisitorId()
                        + " is already checked out.");
            }

            LocalTime outTime = LocalTime.now();
            long minutes = calculateDuration(v.getInTime(), outTime);

            boolean updated = visitorDAO.checkOut(visitorId, outTime, minutes);
            if (!updated) {
                // Someone checked this visitor out at the same moment.
                throw new IllegalStateException("Visitor " + visitorId + " is already checked out.");
            }

            v.setOutTime(outTime);
            v.setTotalDuration((int) minutes);
            v.setStatus("OUT");
            return v;

        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /**
     * Calculates the duration between in time and out time in MINUTES.
     * Example: 10:30 -> 11:45 gives 75 minutes.
     * If the clock went backwards (visit across midnight) the result is 0.
     */
    public static long calculateDuration(LocalTime inTime, LocalTime outTime) {
        long minutes = Duration.between(inTime, outTime).toMinutes();
        return Math.max(0, minutes);
    }

    // =========================================================
    // View / Search
    // =========================================================

    public List<Visitor> getAllVisitors() {
        try {
            return visitorDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    public List<Visitor> getInsideVisitors() {
        try {
            return visitorDAO.findInside();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /** Returns the newest visitor records, newest first (dashboard). */
    public List<Visitor> getRecentVisitors(int limit) {
        try {
            return visitorDAO.findRecent(limit);
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /** Searches by visitor ID, name or phone. */
    public List<Visitor> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter an ID, name or phone number to search.");
        }
        try {
            return visitorDAO.search(keyword.trim());
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    // =========================================================
    // Dashboard statistics
    // =========================================================

    public int countVisitorsToday() {
        try {
            return visitorDAO.countVisitorsToday();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /** Total number of stored visitor records (all time). */
    public int countAll() {
        try {
            return visitorDAO.countAll();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /** Total number of completed visits (all time). */
    public int countCompleted() {
        try {
            return visitorDAO.countCompleted();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    /**
     * Returns the number of visits for each of the last {@code days} days
     * (today included). Days without visits are returned with 0 so the
     * dashboard chart always shows a continuous range.
     */
    public Map<LocalDate, Integer> getVisitCountsForLastDays(int days) {
        int safeDays = Math.max(1, days);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(safeDays - 1L);
        try {
            Map<LocalDate, Integer> stored = visitorDAO.countByDay(from, today);
            Map<LocalDate, Integer> result = new LinkedHashMap<>();
            for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
                result.put(day, stored.getOrDefault(day, 0));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    public int countInside() {
        try {
            return visitorDAO.countInside();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    public int countCompletedToday() {
        try {
            return visitorDAO.countCompletedToday();
        } catch (SQLException e) {
            throw new RuntimeException(friendlyDatabaseMessage(e), e);
        }
    }

    // =========================================================
    // Validation
    // =========================================================

    /**
     * Checks the form values without touching the database.
     * The UI uses this for inline, per-field feedback before saving.
     *
     * @return a friendly message describing the first problem, or null when
     *         the values are acceptable.
     */
    public String validateInput(String name, String phone, String personToMeet) {
        try {
            validateName(name);
            validatePhone(phone);
            if (personToMeet == null || personToMeet.trim().isEmpty()) {
                return "Please enter the person to meet.";
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /** A visitor name is required and cannot be blank. */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Visitor name is required. Please enter a name.");
        }
    }

    /**
     * Validates the phone number.
     * Accepts digits with optional spaces or dashes, for example:
     *   9876543210  |  98765-43210  |  +91 98765 43210
     *
     * @return the cleaned phone number that will be saved.
     */
    private String validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required. Please enter a phone number.");
        }
        String cleaned = phone.trim().replace(" ", "").replace("-", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (!cleaned.matches("[0-9]{7,15}")) {
            throw new IllegalArgumentException(
                    "Invalid phone number.\nUse 7 to 15 digits, for example: 9876543210");
        }
        return cleaned;
    }

    // =========================================================
    // Error messages
    // =========================================================

    /**
     * Returns a message that a normal user can understand (no stack traces and
     * no raw SQL text). Connection problems already arrive as friendly text
     * from DBConnection; anything else is logged and replaced with a plain
     * explanation.
     */
    private String friendlyDatabaseMessage(SQLException e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "Unexpected database error. Please try again.";
        }

        String lower = msg.toLowerCase();
        boolean alreadyFriendly = lower.contains("mysql") && lower.contains("server")
                || lower.contains("cannot connect")
                || lower.contains("does not exist")
                || lower.contains("secure connection");
        if (alreadyFriendly) {
            return msg;
        }

        // Keep the technical detail for developers, not for the visitor.
        System.err.println("[VisitorService] Database error: " + msg);
        return "The database could not complete that request. "
                + "Please try again, and check that MySQL is still running.";
    }
}
