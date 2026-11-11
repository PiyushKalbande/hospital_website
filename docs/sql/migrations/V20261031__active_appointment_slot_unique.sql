-- Safe to re-run against MySQL after taking a backup. Run before deploying the
-- application update when possible; the checks also handle Hibernate adding the
-- new column/index first during ddl-auto=update.
-- Cancelled and no-show appointments stay in appointment history but no longer
-- reserve their original doctor/date/time/visit-mode combination.

SET @migration_sql = (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE appointment DROP INDEX uk_appt_slot',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'uk_appt_slot'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE appointment ADD COLUMN active_slot_key VARCHAR(100) NULL',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND column_name = 'active_slot_key'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE appointment
SET active_slot_key = CASE
    WHEN doctor_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW')
        THEN CONCAT(
            doctor_id, '|', appointment_date, '|',
            TIME_FORMAT(slot_start, '%H:%i:%s'), '|', visit_mode)
    ELSE NULL
END;

SET @migration_sql = (
    SELECT IF(COUNT(*) = 0,
              'CREATE UNIQUE INDEX uk_appt_active_slot ON appointment (active_slot_key)',
              'SELECT 1')
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'appointment'
      AND index_name = 'uk_appt_active_slot'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;
