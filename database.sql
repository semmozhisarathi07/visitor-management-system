-- ============================================================
-- Visitor Management System - MySQL setup script
-- ============================================================
-- HOW TO USE (choose one way):
--
--   Way 1 (GUI): Open MySQL Workbench, paste this whole file,
--                click the lightning bolt (Execute).
--
--   Way 2 (terminal): run
--                mysql -u root -p < database.sql
--
-- You only need to run this file ONCE.
-- It is safe to run again: it will not delete your data.
-- ============================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS visitor_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Switch to that database
USE visitor_management;

-- 3. Create the visitors table
CREATE TABLE IF NOT EXISTS visitors (
    visitor_id      INT AUTO_INCREMENT PRIMARY KEY,
    visitor_name    VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    person_to_meet  VARCHAR(100) NOT NULL,
    reason          VARCHAR(200) NULL,
    visit_date      DATE         NOT NULL,
    in_time         TIME         NOT NULL,
    out_time        TIME         NULL,
    total_duration  INT          NULL COMMENT 'Total duration in MINUTES',
    status          VARCHAR(10)  NOT NULL DEFAULT 'INSIDE' COMMENT 'INSIDE or OUT'
);

-- ============================================================
-- Example of how a record looks after registering and
-- checking out a visitor (you normally do NOT run this):
--
-- INSERT INTO visitors
--     (visitor_name, phone, person_to_meet, reason, visit_date, in_time, out_time, total_duration, status)
-- VALUES
--     ('Ravi Kumar', '9876543210', 'Mrs. Sharma', 'Document submission',
--      '2026-10-05', '10:30:00', '11:45:00', 75, 'OUT');
-- ============================================================
