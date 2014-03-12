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
