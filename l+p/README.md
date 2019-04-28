# Gatling simulation execution examples

## Development
```
# run individual simulations
mvn gatling:execute -Dusers=3000 -Duri=http://localhost:11515 -Dsqlcommander.SelectSimulation

# run all simulations
mvn clean gatling:execute -Dusers=3000
```

## Standalone
```
# build jar
mvn clean package

# run an individual simulation
scripts/runSimulation.sh target/lp-test-sqlcommander-0.0.1.jar \
    http://localhost:11515 sqlcommander.SelectSimulation 100

# run the predefined test
scripts/test.sh
```
