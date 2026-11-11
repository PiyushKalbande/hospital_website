-- One-time migration for existing MySQL databases.
-- Run only if app_user.doctor_id does not already exist.
-- The fresh-database schema is also defined in ../schema-mysql.sql.
ALTER TABLE app_user
    ADD COLUMN doctor_id BIGINT NULL,
    ADD CONSTRAINT uk_app_user_doctor UNIQUE (doctor_id),
    ADD CONSTRAINT FK_user_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id);