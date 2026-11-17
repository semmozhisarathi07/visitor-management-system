package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import database.DBConnection;
import model.Visitor;

/**
 * VisitorDAO.java
 *
 * DAO = Data Access Object.
 * This class is the ONLY class that talks SQL to MySQL.
 * Every query uses PreparedStatement (never string concatenation),
 * which keeps the code safe from SQL injection.
 *
 * The UI never calls this class directly - it always goes through VisitorService.
 */
public class VisitorDAO {

    /*
     * Some installations store in_time / out_time as TIME columns (see
     * database.sql), others as DATETIME columns (a visit moment). The DAO
     * checks the real column type once and then sends the matching JDBC type,
     * so both layouts keep working without any schema change.
     */
    private static Boolean inTimeIsDateTime;
    private static Boolean outTimeIsDateTime;

    /**
     * Inserts a new visitor and returns the same visitor
     * with the automatically generated visitor_id.
     */
    public int insertVisitor(Visitor v) throws SQLException {
        String sql = "INSERT INTO visitors "
                + "(visitor_name, phone, person_to_meet, reason, visit_date, in_time, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, v.getVisitorName());
            ps.setString(2, v.getPhone());
            ps.setString(3, v.getPersonToMeet());
            ps.setString(4, v.getReason());
            ps.setDate(5, Date.valueOf(v.getVisitDate()));
            setMoment(ps, 6, conn, "in_time", v.getVisitDate(), v.getInTime());
            ps.setString(7, v.getStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Visitor was saved but no ID was returned.");
        }
    }

    /** Returns all visitors, newest first. */
    public List<Visitor> findAll() throws SQLException {
        String sql = "SELECT * FROM visitors ORDER BY visitor_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return readRows(conn, rs);
        }
    }

    /** Returns every visitor that is currently inside the building. */
    public List<Visitor> findInside() throws SQLException {
        String sql = "SELECT * FROM visitors WHERE status = 'INSIDE' ORDER BY visitor_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return readRows(conn, rs);
        }
    }

    /**
     * Searches visitors by ID, name or phone.
     * The keyword is matched against visitor_id, visitor_name and phone.
     */
    public List<Visitor> search(String keyword) throws SQLException {
        int id = -1;
        try {
            id = Integer.parseInt(keyword.trim());
        } catch (NumberFormatException ignored) {
            // The keyword is not a number - search only by name and phone.
        }

        String sql = "SELECT * FROM visitors "
                + "WHERE visitor_id = ? OR visitor_name LIKE ? OR phone LIKE ? "
                + "ORDER BY visitor_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return readRows(conn, rs);
            }
        }
    }

    /** Finds one visitor by ID. Returns null when the visitor does not exist. */
    public Visitor findById(int visitorId) throws SQLException {
        String sql = "SELECT * FROM visitors WHERE visitor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, visitorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(conn, rs);
                }
            }
        }
        return null;
    }

    /**
     * Checks a visitor out.
     * The WHERE status = 'INSIDE' condition guarantees that a visitor
     * who is already OUT can never be checked out a second time.
     *
     * @return true when one row was updated, false when the visitor was already OUT.
     */
    public boolean checkOut(int visitorId, LocalTime outTime, long durationMinutes) throws SQLException {
        String sql = "UPDATE visitors SET out_time = ?, total_duration = ?, status = 'OUT' "
                + "WHERE visitor_id = ? AND status = 'INSIDE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setMoment(ps, 1, conn, "out_time", LocalDate.now(), outTime);
            ps.setLong(2, durationMinutes);
            ps.setInt(3, visitorId);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- Column type handling ----------

    /**
     * Sends a time value as either a TIME or a DATETIME parameter, depending on
     * how the column is defined in the database.
     */
    private void setMoment(PreparedStatement ps, int index, Connection conn,
                           String column, LocalDate date, LocalTime time) throws SQLException {
        boolean dateTimeColumn = isDateTimeColumn(conn, column);
        if (dateTimeColumn) {
            // LocalDateTime is sent exactly as written (no time-zone conversion),
            // so the stored value always matches the clock the user saw.
            LocalDate day = date == null ? LocalDate.now() : date;
            ps.setObject(index, LocalDateTime.of(day, time));
        } else {
            ps.setTime(index, Time.valueOf(time));
        }
    }

    /** Reads a TIME or DATETIME column as a LocalTime (no time-zone conversion). */
    private LocalTime readMoment(Connection conn, ResultSet rs, String column) throws SQLException {
        if (isDateTimeColumn(conn, column)) {
            LocalDateTime moment = rs.getObject(column, LocalDateTime.class);
            return moment == null ? null : moment.toLocalTime();
        }
        Time time = rs.getTime(column);
        return time == null ? null : time.toLocalTime();
    }

    /**
     * Detects (once) whether a column is a DATETIME/TIMESTAMP column.
     * Falls back to TIME when the metadata cannot be read.
     */
    private boolean isDateTimeColumn(Connection conn, String column) {
        if (inTimeIsDateTime == null || outTimeIsDateTime == null) {
            inTimeIsDateTime = lookupDateTimeColumn(conn, "in_time");
            outTimeIsDateTime = lookupDateTimeColumn(conn, "out_time");
        }
        if ("out_time".equalsIgnoreCase(column)) {
            return Boolean.TRUE.equals(outTimeIsDateTime);
        }
        return Boolean.TRUE.equals(inTimeIsDateTime);
    }

    private boolean lookupDateTimeColumn(Connection conn, String column) {
        String sql = "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'visitors' AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String type = rs.getString(1);
                    return "datetime".equalsIgnoreCase(type) || "timestamp".equalsIgnoreCase(type);
                }
            }
        } catch (SQLException ignored) {
            // Unknown metadata: assume the documented TIME layout.
        }
        return false;
    }

    /** Returns the newest visitors (used by the dashboard activity list). */
    public List<Visitor> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM visitors ORDER BY visitor_id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                return readRows(conn, rs);
            }
        }
    }

    // ---------- Dashboard counts ----------

    /** Total number of visitor records ever stored. */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM visitors";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Total number of completed (checked out) visits. */
    public int countCompleted() throws SQLException {
        String sql = "SELECT COUNT(*) FROM visitors WHERE status = 'OUT'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Counts the visits per day between two dates (inclusive).
     * Days without any visit are simply missing from the map.
     */
    public Map<LocalDate, Integer> countByDay(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT visit_date, COUNT(*) AS total FROM visitors "
                + "WHERE visit_date BETWEEN ? AND ? "
                + "GROUP BY visit_date ORDER BY visit_date";

        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getDate("visit_date").toLocalDate(), rs.getInt("total"));
                }
            }
        }
        return counts;
    }

    public int countVisitorsToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM visitors WHERE visit_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countInside() throws SQLException {
        String sql = "SELECT COUNT(*) FROM visitors WHERE status = 'INSIDE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int countCompletedToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM visitors WHERE status = 'OUT' AND visit_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ---------- Helper methods ----------

    /** Converts all rows of a ResultSet into a list of Visitor objects. */
    private List<Visitor> readRows(Connection conn, ResultSet rs) throws SQLException {
        List<Visitor> visitors = new ArrayList<>();
        while (rs.next()) {
            visitors.add(mapRow(conn, rs));
        }
        return visitors;
    }

    /** Converts one ResultSet row into a Visitor object. */
    private Visitor mapRow(Connection conn, ResultSet rs) throws SQLException {
        Visitor v = new Visitor();
        v.setVisitorId(rs.getInt("visitor_id"));
        v.setVisitorName(rs.getString("visitor_name"));
        v.setPhone(rs.getString("phone"));
        v.setPersonToMeet(rs.getString("person_to_meet"));
        v.setReason(rs.getString("reason"));
        v.setVisitDate(rs.getDate("visit_date").toLocalDate());

        v.setInTime(readMoment(conn, rs, "in_time"));
        v.setOutTime(readMoment(conn, rs, "out_time"));

        int duration = rs.getInt("total_duration");
        v.setTotalDuration(rs.wasNull() ? null : duration);

        v.setStatus(rs.getString("status"));
        return v;
    }
}
