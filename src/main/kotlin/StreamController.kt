import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import io.github.rybalkinsd.kohttp.ext.httpGet
import io.javalin.Context
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object StreamController {
    private val streamMap: ConcurrentMap<String, ProxyStream> = ConcurrentHashMap()

    fun getStreamList(ctx: Context) {
        ctx.result(streamMap.keys.joinToString(", "))
    }

    fun getThumbnail(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")
        val streamUrl = streamMap[streamId]?.getInternalUri()

        when (streamUrl) {
            null -> ctx.status(400)
            else -> {
                val response = streamUrl.replace("m3u8", "png").httpGet()
                try {
                    when (response.code()) {
                        200 -> {
                            response.body()?.bytes()?.let {
                                ctx.contentType("image/png")
                                ctx.result(ByteArrayInputStream(it))
                            }
                        }
                        else -> {
                            ctx.status(500)
                        }
                    }
                } finally {
                    response.body()?.close()
                }
            }
        }
    }

    fun createStream(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")

        when (streamId) {
            null -> ctx.status(400)
            !in streamMap -> {
                val streamUrls = ctx.formParams("stream_url")
                if (streamUrls.isEmpty()) {
                    ctx.status(400)
                } else {
                    addStream(streamId, streamUrls)
                    ctx.status(200)
                }
            }
            else -> ctx.status(400)
        }
    }

    fun deleteStream(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")

        when (streamId) {
            null -> ctx.status(400)
            in streamMap -> {
                streamMap.remove(streamId)
                ctx.status(200)
            }
            else -> ctx.status(404)
        }
    }

    fun getStream(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")

        when (streamId) {
            null -> ctx.status(400)
            in streamMap -> {
                streamMap[streamId]?.let {
                    it.updatePlaylist()
                    ctx.contentType("application/vnd.apple.mpegurl")
                    ctx.result(it.synthesize())
                }
            }
            else -> ctx.status(404)
        }
    }

    fun getSubPlaylist(ctx: Context) {
        val streamId: String = ctx.pathParam("stream-id")
        val playlistId: String = ctx.pathParam("playlist-id")

        when (streamId) {
            in streamMap -> {
                streamMap[streamId]?.updatePlaylist()
                val subPlaylist = streamMap[streamId]?.getSubPlaylist(playlistId) ?: return
                ctx.contentType("application/vnd.apple.mpegurl")
                ctx.result(subPlaylist)
            }
            else -> ctx.status(404)
        }
    }

    fun getSegment(ctx: Context) {
        val streamId: String = ctx.pathParam("stream-id")
        val segmentId: String = ctx.pathParam("segment-id")

        when (streamId) {
            in streamMap -> {
                val segment = streamMap[streamId]?.getSegmentURL(segmentId)
                when (segment) {
                    null -> ctx.status(404)
                    else -> {
                        runBlocking {
                            Segments.retrieve(segment)?.let {
                                ctx.result(ByteArrayInputStream(it))
                                ctx.contentType("application/octet-stream")
                            } ?: run {
                                ctx.status(404)
                                println("Failed to retrieve segment $segment")
                            }
                        }
                    }
                }
            }
            else -> ctx.status(404)
        }
    }

    fun addStream(name: String, endpoints: List<String>) {
        streamMap[name] = ProxyStream(name, endpoints)
    }
}
