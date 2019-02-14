import io.github.rybalkinsd.kohttp.ext.httpGet
import io.javalin.Context
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
                streamMap[id]?.generatePlaylist()?.let {
                    println(it)
                    ctx.result(it)
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
                            ctx.redirect(segment)
                            /*ctx.contentType("application/octet-stream")
                            val body = segment.httpGet().body()
                            if (body == null) {
                                println("Body is null")
                            } else {
                                ctx.result(body.byteStream())
                            }*/
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
