# Streams Flows API Demo

Demonstration of the [Streams Flows](https://dataplatform.cloud.ibm.com/docs/content/streaming-pipelines/overview-streaming-pipelines.html) [REST API](https://console.bluemix.net/apidocs/watson-data-api#stream-flows).

JDK 8+ required. `gradlew run`, then point your browser to http://localhost:8080/.

(The server may throw an exception on Windows, depending on whether the required encryption algorithms are supported by the platform.)

`gradlew buildDocker` will build a docker image.

The default port can be changed by setting the `PORT` environment variable.
