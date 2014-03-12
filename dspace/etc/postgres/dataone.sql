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