# Romaster README

Romaster is the ui prioject of romking.

To start the application in development mode, import it into your IDE and run the `Application` class. 
You can also start the application from the command line by running: 

```bash
./mvnw
```

To build the application in production mode, run:

```bash
./mvnw -Pproduction package
```

To also build a Docker image, continue by running:

```bash
docker build -t my-application:latest .
```