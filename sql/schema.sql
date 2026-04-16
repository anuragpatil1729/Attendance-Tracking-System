CREATE DATABASE IF NOT EXISTS attendance_db;
USE attendance_db;

CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    roll_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    class VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    date DATE NOT NULL,
    status ENUM('Present', 'Absent', 'Late') NOT NULL DEFAULT 'Present',
    remarks VARCHAR(255),
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    UNIQUE KEY uq_student_date (student_id, date)
);

INSERT IGNORE INTO students (roll_no, name, class) VALUES
    ('S001', 'Aarav Sharma', '10-A'),
    ('S002', 'Priya Mehta', '10-A'),
    ('S003', 'Rohit Verma', '10-B'),
    ('S004', 'Sneha Patil', '10-B'),
    ('S005', 'Karan Joshi', '11-A');
