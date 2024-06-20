# Configuration
Configuration is handled using the typical Spring properties in `application.properties` or `application.yaml` (preferred).

## Configuration layout
```yaml
storage:
  storageLocations:
    # Define "storage pools" here. These are where your weather model files are going to get stored.
    # Add a pool with
    # [name of pool]: [path to pool location]

update:
  updateCheckInterval: 10 # This is how often Imker will search for updated weather data (in minutes).

endpoints:
  simpleWeatherVariables:
    # Define a list of WeatherVariableTypes here, which will be included in the "simple" forecast (see "WeatherVariableType" for more info).

models:
  definedModels: # Define weather models here
    0: # This is the "priority" of the weather model. The lower the number, the more preferred it will be when choosing a weather model.
      meta:
        name: WeatherModel # This is the name of your weather model. This will be used when making API requests.
        friendlyName: My awesome new weather model! # This is the friendly name of your weather model, which will be included in API responses.
        copyright: All rights reserved # This is the copyright string of the weather model, to ensure compliance with licenses like CC BY. This will be included in API responses.
        
      receiver:
        receiverName: changeMe # This is the name of the receiver that will be used to retrieve the weather data (see "Receivers" for more info).
        parserName: changeMe # This is the name of the parser that will be used to parse the weather data (see "Parsers" for more info).
        receiverGroup: default # Optional: Specify a receiver group here. Receivers in the same group won't update at the same time (to avoid issues when connecting to the same host for example).
        
      source:
        updateFrequency: PT15M # How often the weather model gets updated at the source, in Java Duration format. 
        # Add other options for your receiver here (see "Configuring receivers" for more info).
        
      mapping:
        variableMapping:
          # Map variable names/identifiers in your source file to WeatherVariableTypes here (see "WeatherVariableType" for more info).
          # Map with
          # [WeatherVariableType]: [name/identifier in the source file]
        unitMapperFile: /change/me.csv # Path to mapper file for units (see "Unit mapping" for more info).
      
      transforming:
        transformers:
          # Add data transformers here, if necessary (see below for more info).

      storage:
        storageLocationName: changeMe # The name of the storage pool to store the weather data files in (as defined above)
        subFolderName: aSubFolder # Optional: Specify a sub folder in the storage pool.
        policy:
          maxFiles: 1 # Optional: The maximum amount of revisions to store (1 is fine for most applications, Imker does not support handling more at the moment).
          maxAge: PT30M # Optional: The maximum age revisions are allowed to have before being purged.
          
      cache:
        variablesInMemory:
          # Specify WeatherVariableTypes here to cache in memory instead of reading from disk every time.
          # It is recommended to add all defined WeatherVariableTypes here, unless there's a specific reason not to do so.
        ignoredVariables:
          # Specify WeatherVariableTypes to be ignored here.
```

## Further information
### WeatherVariableType
`WeatherVariableTypes` are defined in [`WeatherVariable`](/src/main/kotlin/com/luca009/imker/server/parser/model/WeatherVariable.kt). These are supposed to reflect the different kinds of weather variables one might encounter in different weather models, though this list is not exhaustive.
The purpose of this is to provide a uniform baseline between weather models. 

### Receivers
See also: [Developing: Receiver](Developing.md#receiver)

Receivers are responsible for downloading the weather data from the source. This might be accomplished through HTTP or FTP, and different receivers are available to accomplish this task.
Currently, only an FTP receiver is implemented, namely `ftpSingleFile`, which can handle downloading a specific file from an FTP server.

#### Configuring receivers
##### ftpSingleFile
The ftpSingleFile receiver can be configured as below.
```yaml
ftpHost: # URL of the host
ftpSubFolder: # Sub folder to access on the FTP server
ftpUsername: # Optional: Username for the FTP server
ftpPassword: # Optional: Password for the FTP server
prefix: # Prefix of the file to download 
postfix: # Postfix of the file to download
dateFormat: # Format of the date between the prefix and postfix, in Java DateTimeFormatter format
```

### Parsers
See also: [Developing: Parser](Developing.md#parser)

Parsers parse the downloaded weather data, which usually comes in a NetCDF format.
Currently, only a NetCDF parser is available (though it should theoretically be compatible with all file types [netcdf-java](https://github.com/Unidata/netcdf-java) supports), `netcdf`.

### Unit mapping
An external CSV file gets used to map strings for units one might find in weather datasets to `WeatherVariableUnits` (as defined in [`WeatherVariable`](/src/main/kotlin/com/luca009/imker/server/parser/model/WeatherVariable.kt)).
The first column contains a Regex that is used to match the strings, while the second column contains the matching `WeatherVariableUnit`.
A CSV is used to allow for easy re-use of unit mappings between weather models.