# sql-commander

## Run the application
mvn spring-boot:run

## Test

In another terminal use the following commands
```
# Create a test table
$ script/sql.sh @script/test/create.sql

# Insert a new record
$ script/sql.sh @script/test/insert.sql

# Select all records from test table
$ script/sql.sh script/sql.sh @script/test/select.sql

# Follow new records being created
$ script/sql-select-follow.sh @script/test/select.sql /tmp/output
```