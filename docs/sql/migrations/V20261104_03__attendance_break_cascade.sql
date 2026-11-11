-- One-time, FK-only migration for existing MySQL databases.
-- It preserves all rows and changes only the attendance-break delete rule.
-- The fresh-database schema is defined in ../schema-mysql.sql.
SET @attendance_break_fk = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'doctor_attendance_break'
      AND column_name = 'attendance_id'
      AND referenced_table_name = 'doctor_attendance'
    LIMIT 1
);

SET @drop_fk_sql = IF(
    @attendance_break_fk IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE `doctor_attendance_break` DROP FOREIGN KEY `',
           REPLACE(@attendance_break_fk, '`', '``'), '`')
);
PREPARE drop_fk_statement FROM @drop_fk_sql;
EXECUTE drop_fk_statement;
DEALLOCATE PREPARE drop_fk_statement;

ALTER TABLE doctor_attendance_break
    ADD CONSTRAINT fk_doctor_attendance_break_cascade
    FOREIGN KEY (attendance_id) REFERENCES doctor_attendance (id)
    ON DELETE CASCADE;
