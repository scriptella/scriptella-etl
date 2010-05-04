This example shows how Scriptella can be used as a schema evolution tool.

This example has 2 scripts:
- dbinit - to create the new database
- dbupdate - to upgrade/downgrade the DB from one version to another one.

This sample assumes the following change history:
Version 1: Table Account (Account_ID, Login, Password)
Version 2: Table Account (Account_ID, Login, Password, EMail, Suspended)
Version 3: Table Account (Account_ID, Login, Password, Suspended)
           Table Account_Details (Account_ID, EMail)

Usage
-----
Modify dbinit.properties to specify version of the database you want to create.
Type ant init to create the database schema and fill with initial data.
Modify dbupdate.properties and specify the following parameters:
- Connection settings:
  driver, url, user, password

- Version of the database model you are upgrading from
  version.from=1
- Version of the database model you want to upgrade to
  Downgrading is supported, i.e. to < from
  version.to=3