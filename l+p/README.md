# Gatling simulation execution examples

## Development
```
# run individual simulations
mvn gatling:execute -Dusers=3000 -Duri=http://localhost:15001/cgi-bin \
    -Dgatling.simulationClass=com.net128.test.gatling.wechatin.MessageOutboundSimulation
mvn gatling:execute -Dusers=3000 -Duri=http://localhost:15001 \
    -Dgatling.simulationClass=com.net128.test.gatling.wechatin.MessageInboundSimulation

# run all simulations
mvn clean gatling:execute -Dusers=3000
```

## Standalone
```
# build jar
mvn clean package

# run an individual simulation
./runSimulation.sh target/lp*jar http://localhost:15001/cgi-bin \
    com.net128.test.gatling.wechatin.MessageOutboundSimulation
```
