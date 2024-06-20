# Imker
## About
Imker is a self-hostable weather forecast API powered by the weather models INCA and AROME (provided by GeoSphere Austria), but designed to be easily extensible with new models.

## Getting started
### Compiling
Clone the repository and execute `./gradlew build`.

### Configuring
Configuration can be taken care of using `application.yaml` or `application.properties`, though the YAML format is preferred. For details regarding configuration, see [configuration](/docs/Configuration.md).

### Executing
Execute `./gradlew bootRun`.

## Developing
Imker is split into several, independent components to allow for easy developing.
To get started, see [developing](/docs/Developing.md).

## Endpoints
Imker provides OpenAPI specifications at `/v3/api-docs` or `/v3/api-docs.yaml` when running. These are automatically generated. Sources for the endpoints are located in `com.luca009.imker.server.controllers`.
Alternatively, Swagger UI is available at `/swagger-ui/index.html`.