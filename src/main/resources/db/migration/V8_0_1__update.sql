--
-- We should not delete ps_chargebox entries in table, because UI allows to activate/deactivate PS feature.
-- This situation forces us to decide based on a boolean value, whether it's PS station or not.
--
ALTER TABLE `ps_chargebox`
ADD COLUMN `is_enabled` BOOLEAN NOT NULL DEFAULT FALSE;
