USE attendance_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_mark_attendance$$
CREATE PROCEDURE sp_mark_attendance(
    IN p_student_id INT,
    IN p_date DATE,
    IN p_status VARCHAR(10),
    IN p_remarks VARCHAR(255)
)
BEGIN
    INSERT INTO attendance (student_id, date, status, remarks)
    VALUES (p_student_id, p_date, p_status, p_remarks)
    ON DUPLICATE KEY UPDATE status = p_status,
                            remarks = p_remarks;
END$$

DROP PROCEDURE IF EXISTS sp_get_report$$
CREATE PROCEDURE sp_get_report(
    IN p_from DATE,
    IN p_to DATE,
    IN p_class VARCHAR(50)
)
BEGIN
    SELECT s.roll_no,
           s.name,
           s.class,
           a.date,
           a.status,
           a.remarks
    FROM attendance a
             JOIN students s ON s.id = a.student_id
    WHERE a.date BETWEEN p_from AND p_to
      AND (p_class IS NULL OR p_class = '' OR s.class = p_class)
    ORDER BY a.date DESC, s.roll_no;
END$$

DELIMITER ;
