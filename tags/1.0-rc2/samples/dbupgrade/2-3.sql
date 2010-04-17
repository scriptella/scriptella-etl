--Upgrade from version 2 to version 3 creates a new table
CREATE TABLE Account_Details (
    Account_ID INT,
    EMail VARCHAR(20),
    FOREIGN KEY (Account_ID) REFERENCES Account (Account_ID)
);
--and copy emails from existing accounts into it.
INSERT INTO Account_Details SELECT Account_ID,EMail FROM Account;
--finally the email column is removed from Account table.
ALTER TABLE Account DROP COLUMN EMail;
