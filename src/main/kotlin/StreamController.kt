import io.github.rybalkinsd.kohttp.ext.httpGet
import io.javalin.Context
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object StreamController {
    private val segmentCache = FIFOCache(300)
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
                            val result = segmentCache[segment] ?: run {
                                println("Cache miss for $segment")
                                val request = segment.httpGet()
                                try {
                                    request.body()?.let {
                                        val content = it.bytes()
                                        segmentCache[segment] = content
                                        content
                                    }
                                } finally {
                                    request.body()?.close()
                                }
                            }

                            (result as? ByteArray)?.let {
                                ctx.result(ByteArrayInputStream(it))
                                ctx.contentType("application/octet-stream")
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
