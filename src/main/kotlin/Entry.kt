import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector

fun main(args: Array<String>) {
    // Start web application
    val app= Javalin.create().server{
        Server().apply {
            connectors = arrayOf(ServerConnector(this).apply {
                this.host = "relay"
                this.port = 7000
            })
        }
    }.start()

    app.routes {
        path("relay") {
            get(StreamController::getStreamList)
            path(":stream-id") {
                get(StreamController::getStream)
                post(StreamController::createStream)
                delete(StreamController::deleteStream)
                path(":playlist-id") {
                    get(StreamController::getSubPlaylist)
                }
                path("segment/:segment-id") {
                    get(StreamController::getSegment)
                }
            }
        }
    }

    app.error(404) {
        println("Could not find: ${it.url()}")
    }
}