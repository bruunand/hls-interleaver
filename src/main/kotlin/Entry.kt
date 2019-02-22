import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

fun main(args: Array<String>) {
    if (args.isEmpty()) return

    // Add main stream from args
    StreamController.addStream("main", args)

    // Start web application
    val app = Javalin.create().enableCorsForOrigin("*").start(8000)

    app.routes {
        path("stream/:stream-id") {
            get(StreamController::getStream)
            path("subplaylist/:playlist-id") {
                get(StreamController::getSubplaylist)
            }
            path(":segment-id") {
                get(StreamController::getSegment)
            }
        }
    }

    app.error(404) {
        println("Could not find: ${it.url()}")
    }
}