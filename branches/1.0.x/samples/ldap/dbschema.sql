-- Creates users and roles tables schema suitable for Tomcat JDBCRealm.
drop table users if exists;
create table users (
    user_name varchar(15) not null primary key,
    user_pass varchar(15) not null
);
drop table user_roles if exists;
create table user_roles (
    user_name varchar(15) not null,
    role_name varchar(15) not null,
    primary key (user_name, role_name)
);
