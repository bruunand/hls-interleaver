import io.github.rybalkinsd.kohttp.ext.httpGet
import io.javalin.Context
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object StreamController {
    private val streamMap: ConcurrentMap<String, ProxyStream> = ConcurrentHashMap()

    fun getStream(ctx: Context) {
        val id: String? = ctx.pathParam("stream-id")

        when (id) {
            null -> ctx.status(400)
            in streamMap -> {
                ctx.contentType("application/vnd.apple.mpegurl")
                streamMap[id]?.let {
                    ctx.result(it.internalPlaylist.synthesize())
                }
            }
            else -> ctx.status(404)
        }
    }

    fun getSegment(ctx: Context) {
        val streamId: String? = ctx.pathParam("stream-id")
        val segmentId: String? = ctx.pathParam("segment-id")

        if (streamId == null || segmentId == null) {
            ctx.status(404)
        } else {
            when (streamId) {
                in streamMap -> {
                    val segment = streamMap[streamId]?.getSegmentURL(segmentId)
                    when (segment) {
                        null -> ctx.status(404)
                        else -> {
                            // Either redirect or relay content
                            // ctx.redirect(segment)
                            // TODO: Find a way to close byte stream after
                            ctx.contentType("application/octet-stream")
                            val request = segment.httpGet()
                            try {
                                request.body()?.let { ctx.result(ByteArrayInputStream(it.bytes())) }
                            } finally {
                                request.body()?.close()
                            }
                        }
                    }
                }
                else -> ctx.status(404)
            }
        }
    }

    fun addStream(name: String, endpoints: Array<String>) {
        this.streamMap[name] = ProxyStream(name, endpoints)
    }
}
