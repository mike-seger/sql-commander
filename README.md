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
$ ./sql.sh @script/test/insert.sql

# Select all records from test table
$ ./sql.sh @script/test/select.sql

# Follow new records being created
$ ./sql-select-follow.sh @script/test/select.sql /tmp/output
```

## Running the included bash scripts from the packaged application jar/war

bash <<<$(curl -s localhost:8080/sql.sh) 

## Defining a custom datasource

The pom includes 3 driver dependencies by default: H2, Postgres, Mysql.  
You can use your own drivers instead.  
Without setting any properties, sql-commander will use an H2 in-memory DB configuration.
Below are example configurations which you can add into e.g. config/application.properties

### Postgres
```
spring.profiles.include=customds
spring.custom.datasource.jdbc-url=jdbc:postgresql://server:5432/commanderdb
spring.custom.datasource.username=dbuser
spring.custom.datasource.password=dbpass
spring.custom.datasource.driver-class-name=org.postgresql.Driver
```
