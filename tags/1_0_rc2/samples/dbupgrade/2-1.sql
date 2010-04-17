--Downgrade from version 2 to version 1 removes the Suspended column
--introduced in version 2.
ALTER TABLE Account DROP COLUMN Suspended;