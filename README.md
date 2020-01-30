# Railway station
Station is Railways client-side application. It polls central for new tasks and executes the algorithm defined in the train specified in the task. The train is implemented as a docker image, which is pulled by station. Afterwards, a container is started for each task and the *runStation.sh* or *runMaster.sh* of the container is run depending on the type of task. The results of the task are sent back to central which will ultimately result in a final result or new tasks being created which will again be polled.

# Authentication
Clients use Keycloak to authenticate by means of a client secret. The client name and secret can be defined in the application.yml and should correspond with the entries defined in Keycloak.

# Running and building station
Station is a standard Spring-boot application and can be built with *mvn clean package*.

