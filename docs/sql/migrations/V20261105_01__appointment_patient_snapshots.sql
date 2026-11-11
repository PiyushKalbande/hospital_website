-- One-time migration for existing MySQL databases.
-- Run only when the two snapshot columns are absent.
-- Existing rows are backfilled from the current patient profile; historical names
-- that were overwritten before this migration cannot be reconstructed.
ALTER TABLE appointment
    ADD COLUMN patient_name_snapshot VARCHAR(160) NULL,
    ADD COLUMN patient_phone_snapshot VARCHAR(30) NULL;

UPDATE appointment a
JOIN patient p ON p.id = a.patient_id
SET a.patient_name_snapshot = p.full_name,
    a.patient_phone_snapshot = p.phone
WHERE a.patient_name_snapshot IS NULL OR a.patient_phone_snapshot IS NULL;
