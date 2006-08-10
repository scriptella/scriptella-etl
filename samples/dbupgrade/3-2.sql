--Downgrade from version 3 to version 2 restores the email column.
ALTER TABLE Account ADD COLUMN EMail VARCHAR(20);
--and copy emails from account_details to this column
UPDATE Account a SET EMail=(SELECT EMAIL FROM Account_Details d WHERE d.Account_ID=a.Account_ID);
--finally drop the table which is not used in version 2
DROP TABLE Account_Details;
