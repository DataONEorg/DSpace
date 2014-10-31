—-----------------
-- Versioning Improvements (METS/ORE serializations, handle id, contained bitstreams and restoration)
—----------------- 
ALTER TABLE versionitem ALTER COLUMN version_summary TYPE TEXT;
ALTER TABLE versionitem ADD bitstream_id INTEGER REFERENCES Bitstream(bitstream_id);
ALTER TABLE versionitem ADD ore_bitstream_id INTEGER REFERENCES Bitstream(bitstream_id);
ALTER TABLE versionitem ADD COLUMN version_log TEXT;
ALTER TABLE versionitem ADD COLUMN handle VARCHAR(255);

CREATE SEQUENCE version2bitstream_seq;

CREATE TABLE Version2Bitstream
(
  id            INTEGER PRIMARY KEY,
  version_id INTEGER REFERENCES VersionItem(versionitem_id),
  bitstream_id INTEGER REFERENCES Bitstream(bitstream_id)
);

—-----------------
-- DROP Item ID Constraint (for restoring deleted Items from Version History) 
—----------------- 
ALTER TABLE versionitem DROP CONSTRAINT versionitem_item_id_fkey;

—-----------------
-- Bitstream Improvements (Created, last modified Triggers, char based uuid column)
—-----------------
create EXTENSION "uuid-ossp";
alter table bitstream add column uuid VARCHAR(36) default uuid_generate_v4();
alter table bitstream add column create_date timestamp default current_timestamp;
alter table bitstream add column last_modified_date timestamp default current_timestamp;

drop trigger log_create_date on bitstream;
drop function log_create_date();

CREATE FUNCTION log_create_date() RETURNS TRIGGER AS $_2$
BEGIN
IF ( NEW.create_date IS NULL) THEN
UPDATE bitstream SET create_date = now() WHERE bitstream.bitstream_id = NEW.bitstream_id;
END IF;
IF ( NEW.last_modified_date IS NULL  OR OLD. last_modified_date IS NULL) THEN
UPDATE bitstream SET last_modified_date = now() WHERE bitstream.bitstream_id = NEW.bitstream_id;
ELSIF ( OLD.last_modified_date != now() ) THEN
UPDATE bitstream SET last_modified_date = now() WHERE bitstream.bitstream_id = NEW.bitstream_id;
END IF;
RETURN NEW;
END $_2$ LANGUAGE 'plpgsql';

CREATE TRIGGER log_create_date AFTER UPDATE ON bitstream FOR EACH ROW EXECUTE PROCEDURE log_create_date();