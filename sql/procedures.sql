USE attendance_db;

DELIMITER $$

DROP PROCEDURE IF EXISTS sp_lecture_upsert$$
CREATE PROCEDURE sp_lecture_upsert(
    IN p_officer_id INT,
    IN p_user_id INT,
    IN p_session_id INT,
    IN p_status VARCHAR(20),
    IN p_remarks VARCHAR(255),
    IN p_reason TEXT
)
BEGIN
    INSERT INTO attendance (user_id, session_id, status, remarks, ip_address, device_fingerprint, sync_status, local_id)
    VALUES (p_user_id, p_session_id, p_status, p_remarks, NULL, NULL, 'synced', UUID())
    ON DUPLICATE KEY UPDATE status = VALUES(status), remarks = VALUES(remarks);

    INSERT INTO audit_log(officer_id, action, target_table, target_id, old_value, new_value, reason)
    VALUES (p_officer_id, 'update', 'attendance', p_session_id, '', CONCAT(p_status, ' | ', IFNULL(p_remarks,'')), p_reason);
END$$

DELIMITER ;
