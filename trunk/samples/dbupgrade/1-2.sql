--Upgrading from version 1 to version 2
--Create 2 addtional columns.
ALTER TABLE Account ADD COLUMN Suspended BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE Account ADD COLUMN EMail VARCHAR(20);