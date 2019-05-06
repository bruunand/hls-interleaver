import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import java.util.*

fun main(args: Array<String>) {
    // Start web application
    val app= Javalin.create().server{
        Server().apply {
            connectors = arrayOf(ServerConnector(this).apply {
                this.host = "0.0.0.0"
                this.port = 7000
            })
        }
    }.start()

    StreamController.addStream("main", Arrays.asList("https://envue.me/hls/test.m3u8"))

    app.routes {
        path("relay") {
            get(StreamController::getStreamList)
            path(":stream-id") {
                path("thumbnail") {
                    get(StreamController::getThumbnail)
                }
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