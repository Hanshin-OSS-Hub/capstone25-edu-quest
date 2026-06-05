ALTER TABLE submission_status
  ADD COLUMN uuid CHAR(36) NULL AFTER id;

UPDATE submission_status
SET uuid = UUID()
WHERE uuid IS NULL;

ALTER TABLE submission_status
  MODIFY uuid CHAR(36) NOT NULL;

CREATE UNIQUE INDEX submission_status_uuid_unique ON submission_status (uuid ASC);
