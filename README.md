# sql-commander

## Run the application
mvn spring-boot:run

## Test

In a separate terminal use the following commands
```
cd  src/main/resources/
# Create a test table
$ ./sql.sh @script/test/create.sql

# Insert a new record
$ .sql.sh @script/test/insert.sql

# Select all records from test table
$ ./sql.sh script/sql.sh @script/test/select.sql

# Follow new records being created
$ ./sql-select-follow.sh @script/test/select.sql /tmp/output
```

## Running running the bash scripts from the packaged application jar/war

bash <<<$(curl -s localhost:8080/sql.sh) 