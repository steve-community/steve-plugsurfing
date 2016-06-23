ALTER TABLE ps_session
  ADD COLUMN event_timestamp TIMESTAMP(6) NULL,
  ADD COLUMN event_status VARCHAR(255) NOT NULL;
