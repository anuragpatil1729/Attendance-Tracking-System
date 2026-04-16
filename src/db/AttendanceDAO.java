package db;

import model.RollCall;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for attendance and student persistence queries.
 */
public class AttendanceDAO {

    /**
     * Loads all student rows.
     */
    public List<RollCall> getAllStudents() {
        String sql = "SELECT id, roll_no, name, class FROM students ORDER BY roll_no";
        List<RollCall> students = new ArrayList<>();
        try {
            Connection connection = DBConnection.getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RollCall rc = new RollCall();
                    rc.setStudentId(rs.getInt("id"));
                    rc.setRollNo(rs.getString("roll_no"));
                    rc.setName(rs.getString("name"));
                    rc.setStudentClass(rs.getString("class"));
                    students.add(rc);
                }
            }
            return students;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch students", ex);
        }
    }

    /**
     * Upserts attendance for a student/date using stored procedure.
     */
    public void markAttendance(int studentId, LocalDate date, String status, String remarks) {
        String sql = "{CALL sp_mark_attendance(?, ?, ?, ?)}";
        try {
            Connection connection = DBConnection.getConnection();
            try (CallableStatement cs = connection.prepareCall(sql)) {
                cs.setInt(1, studentId);
                cs.setDate(2, Date.valueOf(date));
                cs.setString(3, status);
                cs.setString(4, remarks);
                cs.execute();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to mark attendance", ex);
        }
    }

    /**
     * Returns attendance rows for a particular date.
     */
    public List<RollCall> getAttendanceByDate(LocalDate date) {
        String sql = """
                SELECT a.id, a.student_id, s.roll_no, s.name, s.class, a.date, a.status, a.remarks
                FROM attendance a
                JOIN students s ON s.id = a.student_id
                WHERE a.date = ?
                ORDER BY s.roll_no
                """;
        List<RollCall> rows = new ArrayList<>();
        try {
            Connection connection = DBConnection.getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(mapRollCall(rs));
                    }
                }
            }
            return rows;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch attendance by date", ex);
        }
    }

    /**
     * Returns report data for a date range and optional class.
     */
    public List<RollCall> getReport(LocalDate from, LocalDate to, String cls) {
        String sql = "{CALL sp_get_report(?, ?, ?)}";
        List<RollCall> rows = new ArrayList<>();
        try {
            Connection connection = DBConnection.getConnection();
            try (CallableStatement cs = connection.prepareCall(sql)) {
                cs.setDate(1, Date.valueOf(from));
                cs.setDate(2, Date.valueOf(to));
                if (cls == null || cls.isBlank() || "All".equalsIgnoreCase(cls)) {
                    cs.setNull(3, Types.VARCHAR);
                } else {
                    cs.setString(3, cls);
                }
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        RollCall rc = new RollCall();
                        rc.setRollNo(rs.getString("roll_no"));
                        rc.setName(rs.getString("name"));
                        rc.setStudentClass(rs.getString("class"));
                        rc.setDate(rs.getDate("date").toLocalDate());
                        rc.setStatus(rs.getString("status"));
                        rc.setRemarks(rs.getString("remarks"));
                        rows.add(rc);
                    }
                }
            }
            return rows;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch report", ex);
        }
    }

    /**
     * Returns status-wise attendance counts for a student.
     */
    public Map<String, Integer> getSummary(int studentId) {
        String sql = """
                SELECT status, COUNT(*) AS total
                FROM attendance
                WHERE student_id = ?
                GROUP BY status
                """;
        Map<String, Integer> summary = new HashMap<>();
        summary.put("Present", 0);
        summary.put("Absent", 0);
        summary.put("Late", 0);

        try {
            Connection connection = DBConnection.getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        summary.put(rs.getString("status"), rs.getInt("total"));
                    }
                }
            }
            return summary;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch attendance summary", ex);
        }
    }

    /**
     * Returns distinct class names.
     */
    public List<String> getAllClasses() {
        String sql = "SELECT DISTINCT class FROM students WHERE class IS NOT NULL AND class <> '' ORDER BY class";
        List<String> classes = new ArrayList<>();
        try {
            Connection connection = DBConnection.getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classes.add(rs.getString("class"));
                }
            }
            return classes;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch classes", ex);
        }
    }

    /**
     * Calculates present percentage between dates (inclusive).
     */
    public double getAttendancePercentage(int studentId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                    SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) AS present_count,
                    COUNT(*) AS total_count
                FROM attendance
                WHERE student_id = ?
                  AND date BETWEEN ? AND ?
                """;
        try {
            Connection connection = DBConnection.getConnection();
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, studentId);
                ps.setDate(2, Date.valueOf(from));
                ps.setDate(3, Date.valueOf(to));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt("total_count");
                        int present = rs.getInt("present_count");
                        return total == 0 ? 0.0 : (present * 100.0) / total;
                    }
                }
            }
            return 0.0;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to calculate attendance percentage", ex);
        }
    }

    private RollCall mapRollCall(ResultSet rs) throws SQLException {
        RollCall rc = new RollCall();
        rc.setId(rs.getInt("id"));
        rc.setStudentId(rs.getInt("student_id"));
        rc.setRollNo(rs.getString("roll_no"));
        rc.setName(rs.getString("name"));
        rc.setStudentClass(rs.getString("class"));
        Date date = rs.getDate("date");
        if (date != null) {
            rc.setDate(date.toLocalDate());
        }
        rc.setStatus(rs.getString("status"));
        rc.setRemarks(rs.getString("remarks"));
        return rc;
    }
}
