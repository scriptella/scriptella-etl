This example demonstrates migration between LDAP and Database.
In particular this example shows how to migrate users/roles between Tomcat JDBC/Datasoure And LDAP realms.

!Important!
    This example needs JDBC-LDAP bridge. Download http://www.qa.octetstring.com/downloads/jdbcldap/jdbcldap20en.zip,
    extract jdbcLdapJldap.jar and save in examples lib directory given jdbcldap.jar name.


OpenLDAP configuration
----------------------
Modify the default slapd.conf with the following snippets:

include		./schema/core.schema
include		./schema/cosine.schema
include		./schema/inetorgperson.schema

..................

suffix		"dc=scriptella"
rootdn		"cn=root,dc=scriptella"

Migration of LDAP data to database
----------------------------------
It is assumed that the directory is populated with the following data (LDIF format):
# top-level entry
dn: dc=scriptella
objectclass: dcObject
objectclass: organization
o: Company
dc: scriptella

# Define an entry to contain people
dn: ou=people,dc=scriptella
objectClass: organizationalUnit
ou: people

# Define a user entry for Janet Jones
dn: uid=jjones,ou=people,dc=scriptella
objectClass: inetOrgPerson
uid: jjones
sn: jones
cn: janet jones
mail: j.jones@mycompany.com
userPassword: janet

# Define a user entry for Fred Bloggs
dn: uid=fbloggs,ou=people,dc=scriptella
objectClass: inetOrgPerson
uid: fbloggs
sn: bloggs
cn: fred bloggs
mail: f.bloggs@mycompany.com
userPassword: fred

# Define an entry to contain LDAP groups
dn: ou=groups,dc=scriptella
objectClass: organizationalUnit
ou: groups

# Define an entry for the "admin" role
dn: cn=admin,ou=groups,dc=scriptella
objectClass: groupOfUniqueNames
cn: admin
uniqueMember: uid=jjones,ou=people,dc=scriptella
uniqueMember: uid=fbloggs,ou=people,dc=scriptella

# Define an entry for the "developer" role
dn: cn=developer,ou=groups,dc=scriptella
objectClass: groupOfUniqueNames
cn: developer
uniqueMember: uid=fbloggs,ou=people,dc=scriptella

You may check for required data presence in LDAP by running:
ldapsearch -b "dc=scriptella" "(objectclass=*)"

Execute ldap2db.etl.xml script to migrate data from LDAP to HSQLDB database named "outdb".
Note: Use command line "scriptella ldap2db" to run the script.

Migration of database data to LDAP.
-----------------------------------
db2ldap.etl.xml script migrates users and roles from an in-memory database to LDAP.