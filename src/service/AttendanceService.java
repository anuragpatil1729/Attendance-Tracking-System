package service;

import db.AttendanceDAO;
import model.RollCall;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Service layer for validation and business workflows.
 */
public class AttendanceService {
    private static final Set<String> VALID_STATUSES = Set.of("Present", "Absent", "Late");

    private final AttendanceDAO attendanceDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public List<RollCall> getAllStudents() {
        return attendanceDAO.getAllStudents();
    }

    public List<String> getAllClasses() {
        return attendanceDAO.getAllClasses();
    }

    public List<RollCall> getAttendanceByDate(LocalDate date) {
        return attendanceDAO.getAttendanceByDate(date);
    }

    public List<RollCall> generateSummaryReport(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return attendanceDAO.getReport(from, to, null);
    }

    public List<RollCall> generateSummaryReport(LocalDate from, LocalDate to, String cls) {
        validateDateRange(from, to);
        return attendanceDAO.getReport(from, to, cls);
    }

    /**
     * Marks one attendance row after validation.
     */
    public void markAttendance(int studentId, LocalDate date, String status, String remarks) {
        validateAttendanceInput(date, status);
        attendanceDAO.markAttendance(studentId, date, status, remarks);
    }

    /**
     * Marks multiple records in sequence.
     */
    public void markBulk(List<RollCall> records) {
        for (RollCall record : records) {
            markAttendance(
                    record.getStudentId(),
                    record.getDate(),
                    record.getStatus(),
                    record.getRemarks()
            );
        }
    }

    public double getAttendancePercentage(int studentId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return attendanceDAO.getAttendancePercentage(studentId, from, to);
    }

    private void validateAttendanceInput(LocalDate date, String status) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future.");
        }
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Status must be Present, Absent, or Late.");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("From and To dates are required.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date.");
        }
    }
}
