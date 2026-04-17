CREATE DATABASE IF NOT EXISTS attendance_db;
USE attendance_db;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120),
    role ENUM('attendee','officer') NOT NULL,
    device_fingerprint VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    subject VARCHAR(120) NOT NULL,
    opened_by INT NOT NULL,
    open_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    close_time TIMESTAMP NULL,
    lock_duration_minutes INT NOT NULL DEFAULT 10,
    is_open TINYINT(1) NOT NULL DEFAULT 1,
    session_type ENUM('practical','lecture') NOT NULL DEFAULT 'practical',
    FOREIGN KEY (opened_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    session_id INT NOT NULL,
    status ENUM('Present','Absent','Late') NOT NULL DEFAULT 'Present',
    remarks VARCHAR(255),
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    sync_status ENUM('synced','pending') NOT NULL DEFAULT 'synced',
    local_id VARCHAR(36) NOT NULL,
    UNIQUE KEY uq_local_id (local_id),
    UNIQUE KEY uq_user_session (user_id, session_id),
    INDEX idx_attendance_user_session (user_id, session_id),
    INDEX idx_attendance_ip_fingerprint (session_id, ip_address, device_fingerprint),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE IF NOT EXISTS device_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    ip_address VARCHAR(45),
    device_fingerprint VARCHAR(255),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    logout_time TIMESTAMP NULL,
    attempt_status ENUM('success','blocked') NOT NULL DEFAULT 'success',
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    officer_id INT NOT NULL,
    action ENUM('create','update','delete','force_edit') NOT NULL,
    target_table VARCHAR(50),
    target_id INT,
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (officer_id) REFERENCES users(id)
);
