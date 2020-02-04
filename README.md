# Railway station
Station is Railways client-side application. It polls central for new tasks and pulls the so called 'train' specified in the task, which is a docker image created by a researcher. Afterwards, a container is started for the train and the task is run by executing either *runStation.sh* or *runMaster.sh*, depending on the type of task. The results of the task are sent back to central which will ultimately result in a final result or new tasks being created which will again be polled.

# Authentication
Clients use Keycloak to authenticate by means of a client secret. The client-id and client-secret can be defined in the application.yml and should correspond with the entries defined in Keycloak. Ask your central administrator for a new client entry in keycloak.

# Running and building station
Station is a standard Spring-boot application and can be built with *mvn clean package*. To run the application, copy the target folder to a desired location after building and run *java -jar station-{VERSION}.jar*. Make sure the *client-id* and *client-secret* are configured in the *application.yml*
 
# Troubleshooting
* *finishConnect(..) failed: Connection refused* \
Check the port number in the specified URL. Either Keycloak or Central cannot be reached by station. By default 9080 is configured for Keycloak and 8080 is configured for Central. 
* *ProductionTaskService    : invalid station name: {STATION_NAME}* \
The client-id in the application.yml is unknown in Central. Either the client-id is configured incorrectly in the application.yml, or the station still needs to be configured to Central. Ask your central administrator for a new Station entry  in Central.