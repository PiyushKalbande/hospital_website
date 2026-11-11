-- One-time migration for existing MySQL databases.
-- Run only if appointment.reschedule_reason does not already exist.
-- The fresh-database schema is also defined in ../schema-mysql.sql.
ALTER TABLE appointment
    ADD COLUMN reschedule_reason VARCHAR(400) NULL AFTER cancellation_reason;
