package me.envue.relay

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*

fun main(args: Array<String>) {
    // Start web application
    val app = Javalin.create().enableCorsForOrigin("*").start(7000)

    app.routes {
        path("stream") {
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