# Developing
## Components
Imker is split into various different components, representing the different areas of the weather data supply chain.
These boil down to:
- [Receivers](#receiver) (downloading the data)
- [Parsers](#parser) (parsing the downloaded data)
- [Caching](#caching)
- [Querying](#querying)
- [Controllers](#controller) (endpoints for outputting the data)

### Receiver
A receiver is responsible for downloading weather data. It's important to note that a receiver is not responsible for storing the data - it merely gets supplied a storage path which specifies where the files should get downloaded to.
Receivers get automatically instantiated based on the configuration of the Spring app.
In an ideal case, implementing a unique receiver should not be necessary, as there are a handful of generic receivers available already, which should be able to handle a variety of situations.

#### Implementing a receiver
Get started by creating a new class in [`com.luca009.imker.server.receiver`](/src/main/kotlin/com/luca009/imker/server/receiver) and implementing the [`DataReceiver`](/src/main/kotlin/com/luca009/imker/server/receiver/model/DataReceiver.kt) interface.
Once the implementation is done, add your new receiver to the `dataReceiver` function in [`DataReceiverConfiguration`](/src/main/kotlin/com/luca009/imker/server/receiver/DataReceiverConfiguration.kt), which is responsible for mapping the receiver names in the Spring configuration to actual instances.
Everything else, like file management, is taken care for you by Imker.

### Parser
Parsers take the downloaded data and parse it into Java data types, usually Doubles.
Again, there are generic parsers available, so it might not be necessary to implement a new one.

#### Implementing a parser
Get started by creating a new class in [`com.luca009.imker.server.parser`](/src/main/kotlin/com/luca009/imker/server/parser) and implementing the [`WeatherDataParser`](/src/main/kotlin/com/luca009/imker/server/parser/model/WeatherDataParser.kt) interface.
Once the implementation is done, create a new factory method for your new parser (see [`NetCdfParserFactoryConfiguration`](/src/main/kotlin/com/luca009/imker/server/parser/NetCdfParserFactoryConfiguration.kt) for reference) and add it to the `weatherDataParserFactory` function in [`WeatherDataParserConfiguration`](/src/main/kotlin/com/luca009/imker/server/parser/WeatherDataParserConfiguration.kt), which is responsible for mapping the parser names in the Spring configuration to actual instances.

### Caching
Caching is taken care of by Imker and there should be no need to change it except for bug fixes or internal improvements.
Nevertheless, caching is implemented in [`com.luca009.imker.server.caching`](/src/main/kotlin/com/luca009/imker/server/caching).

### Querying
The API for querying is implemented in [`WeatherDataQueryService`](/src/main/kotlin/com/luca009/imker/server/queries/model/WeatherDataQueryService.kt).

### Controller
Controllers are responsible for API endpoints available. They are implemented in [`com.luca009.imker.server.controllers`](/src/main/kotlin/com/luca009/imker/server/controllers).