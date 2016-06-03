--
-- Create the Session table for PlugSurfing
--
CREATE TABLE ps_session (
  ps_session_pk  INT(11) AUTO_INCREMENT NOT NULL,
  transaction_pk INT(10) UNSIGNED,
  ocpp_tag_pk    INT(11) NOT NULL,
  connector_pk   INT(11) UNSIGNED NOT NULL,
  PRIMARY KEY (ps_session_pk),
  CONSTRAINT FK_transaction_pk
  FOREIGN KEY (transaction_pk) REFERENCES transaction (transaction_pk)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT FK_ocpp_tag_pk
  FOREIGN KEY (ocpp_tag_pk) REFERENCES ocpp_tag (ocpp_tag_pk)
    ON DELETE CASCADE
    ON UPDATE NO ACTION,
  CONSTRAINT FK_connector_pk
  FOREIGN KEY (connector_pk) REFERENCES connector (connector_pk)
    ON DELETE CASCADE
    ON UPDATE NO ACTION
);

--
-- Create the Additional PlugSurfing Information for ChargeBox table (Station)
--
CREATE TABLE ps_chargebox (
  charge_box_pk INT(11) NOT NULL,
  number_of_connectors INT(11) NOT NULL,
  is_open_24 BOOLEAN NOT NULL,
  is_reservable BOOLEAN,
  floor_level INT(5),
  is_free_charge BOOLEAN,
  total_parking INT(7),
  is_green_power_available BOOLEAN,
  is_plugin_charge BOOLEAN,
  is_roofed BOOLEAN,
  is_private BOOLEAN,
  phone VARCHAR(255) NOT NULL,
  fax VARCHAR(255),
  website VARCHAR(255),
  email VARCHAR(255),
  post_timestamp TIMESTAMP(6) NULL DEFAULT NULL,
  PRIMARY KEY (charge_box_pk),
  CONSTRAINT FK_ps_chargebox
  FOREIGN KEY (charge_box_pk) REFERENCES charge_box(charge_box_pk)
    ON DELETE CASCADE ON UPDATE NO ACTION
);

--
-- Plug surfing table for
-- roaming purpouses (i.e. if it is a foreign id)
-- and if the ocpp_tag was posted to PlugSurfing as rfid value
--
CREATE TABLE ps_ocpp_tag (
  ocpp_tag_pk INT(11) NOT NULL,
  post_timestamp TIMESTAMP(6) NULL DEFAULT NULL,
  vendor_name VARCHAR(30),
  in_session BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (ocpp_tag_pk),
  CONSTRAINT FK_ps_ocpp_tag
  FOREIGN KEY (ocpp_tag_pk) REFERENCES ocpp_tag(ocpp_tag_pk)
    ON DELETE CASCADE ON UPDATE NO ACTION
);
