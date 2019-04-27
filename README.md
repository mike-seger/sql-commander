# sql-commander

## Run the application
mvn spring-boot:run

## Test using included example scripts
In a separate terminal use the following commands
```
$ cd src/main/resources/
$ ./sql.sh @script/test/create.sql  # Create a test table
$ ./sql.sh @script/test/insert.sql  # Insert a new record
$ ./sql.sh @script/test/select.sql  # Select all records from test table
# Follow new records being created
$ ./sql-select-follow.sh @script/test/select.sql /tmp/output
```

## Running the included bash scripts from the packaged application jar/war
```
bash <<<$(curl -s localhost:8080/sql.sh) 
```

## Test using curl/API
```
$ curl -s -X POST -H "Content-Type: text/plain" -d "create table x(val int)" localhost:8080/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:8080/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:8080/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "insert into x values($(date +%s))" localhost:8080/update
$ curl -s -X POST -H "Content-Type: text/plain" -d "select * from x" localhost:8080/select
$ curl -s -X POST -H "Content-Type: text/plain" -d "drop table x" localhost:8080/update
$ curl -s localhost:8080/runscript?resourceUrl=classpath:/static/test/populate.sql
```

## Example datasource definitions
The pom includes 3 driver dependencies by default: H2, Postgres, Mysql/MariaDB.  
You can use your own drivers instead.  
Without setting any properties, sql-commander will use an H2 in-memory DB configuration.  
Below are some example configurations, which you can add into e.g. config/application.properties

### Postgres
```
spring.profiles.include=customds
spring.custom.datasource.jdbc-url=jdbc:postgresql://server1:5432/commanderdb
spring.custom.datasource.driver-class-name=org.postgresql.Driver
```

#### Create a Postgres DB
```
sudo su - postgres
psql
postgres=# alter user commander  with encrypted password 'commanderpass';
postgres=# grant all privileges on database commanderdb to commander;

```

### Mysql/MariaDB
```
spring.profiles.include=customds
spring.custom.datasource.jdbc-url=jdbc:postgresql://server:5432/commanderdb
spring.custom.datasource.username=dbuser
spring.custom.datasource.password=dbpass
spring.custom.datasource.driver-class-name=org.postgresql.Driver
```

#### Create a Mysql DB
```
sudo mysql
MariaDB [(none)]> create database commanderdb default character set utf8 default collate utf8_bin;
MariaDB [(none)]> GRANT ALL PRIVILEGES ON commanderdb.* to commander@'%' IDENTIFIED BY 'commanderpass';
MariaDB [(none)]> GRANT ALL PRIVILEGES ON commanderdb.* to commander@'localhost' IDENTIFIED BY 'commanderpass';
MariaDB [(none)]> flush privileges;
```
