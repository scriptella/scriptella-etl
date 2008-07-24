This example demonstrates a combined usage of Velocity, Janino, 
CSV, XLS driver and SQL.

Janino is used to generate prime numbers and to produce
a virtual rowset for exporting into a database.

Velocity is used to produce HTML report based on a SQL query.

CSV Driver produces CSV file with prime numbers.

XLS Driver produces Excel file with prime numbers.

Script configuration is specified in etl.properties file.

Usage
-----

Just type ant (for running from ant build file) or scriptella (to invoke
without Ant bridge).