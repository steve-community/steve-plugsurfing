CREATE TABLE ps_evco_id (
  evco_id VARCHAR(255) NOT NULL,
  ocpp_id_tag VARCHAR(255) NOT NULL COMMENT 'part of the hash_value that is used as rfid for ocpp operations',
  hash_value VARCHAR(255) NOT NULL,
  hash_algorithm_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (evco_id)
);
