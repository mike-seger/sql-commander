# sql-commander

An SQL utility hosted within a Spring Boot application.  
The main purpose is to provide an SQL client as a single JAR file. It allows to access a DB in environments, where no GUI and no native clients are available.  

## Run the application
mvn spring-boot:run

## Test using included sample scripts
In a separate terminal use the following commands
```
$ cd src/main/resources/static
$ ./sql.sh @test/create.sql  # Create a test table
$ ./sql.sh @test/insert.sql  # Insert a new record
$ ./sql.sh @test/select.sql  # Select all records from test table
# Follow new records being created
$ ./sql-select-follow.sh @script/test/select.sql /tmp/output
$ ./sql.sh @test/drop.sql    # Drop the test table
```

## Running the included bash scripts from the packaged application jar/war
```
bash <<<$(curl -s localhost:11515/sql.sh) 
```

## Test using curl/API
```
$ curl -s -X POST -H "Content-Type: text/plain" -d "create table x(val int)" localhost:11515/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:11515/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:11515/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:11515/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "select * from x" localhost:11515/select
$ curl -s -X POST -H "Content-Type: text/plain" -d "drop table x" localhost:11515/update
$ curl -s localhost:11515/runscript?resourceUrl=classpath:/static/test/populate.sql
```

## Example datasource definitions
The pom includes 3 driver dependencies by default: H2, Postgres, Mysql/MariaDB.  
You can use your own drivers instead.  
Without setting any properties, sql-commander will use an H2 in-memory DB configuration.  
Below are some example configurations, which you can add into e.g. config/application.properties

### Postgres
```
spring.profiles.include=customds
spring.custom.datasource.jdbc-url=jdbc:postgresql://server:5432/commanderdb
spring.custom.datasource.username=dbuser
spring.custom.datasource.password=dbpass
```

#### Create and use Postgres DB
```
$ sudo su - postgres
$ psql
postgres=# alter user commander  with encrypted password 'commanderpass';
postgres=# grant all privileges on database commanderdb to commander;
postgres=# exit
$ psql -h localhost -p 5432 -U commander -d commanderdb
```

### Mysql/MariaDB
```
spring.profiles.include=customds
spring.custom.datasource.jdbc-url=jdbc:mysql://server:3306/commanderdb
spring.custom.datasource.username=dbuser
spring.custom.datasource.password=dbpass
```

#### Create and use Mysql DB
```
$ sudo mysql
MariaDB [(none)]> create database commanderdb default character set utf8 default collate utf8_bin;
MariaDB [(none)]> GRANT ALL PRIVILEGES ON commanderdb.* to commander@'%' IDENTIFIED BY 'commanderpass';
MariaDB [(none)]> GRANT ALL PRIVILEGES ON commanderdb.* to commander@'localhost' IDENTIFIED BY 'commanderpass';
MariaDB [(none)]> flush privileges;
MariaDB [(none)]> exit
$ mysql -u commander -p commanderdb
```

### Other databases

In order to use another database than the default included, you may do one of the following:
- add a dependency to the driver in pom.xml
- add the driver jar in the driver directory (see "Use external driver location")

#### Use external driver location
In order to use an external driver location, you can start the application as follows:
```
java -jar sql-commander.jar
```
This allows to put any additional jdbc drivers into the directory: driver.
This location is defined in loader.properties.
