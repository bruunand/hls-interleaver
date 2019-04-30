import okhttp3.HttpUrl
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

abstract class Playlist {
    abstract fun synthesize(): String

    companion object {
        private inline fun <reified T : Number> parseDelimited(string: String, expectedHeader: String): Number? {
            string.split(':').let {
                if (it.size != 2 || it[0] != expectedHeader) return null

                return when (T::class) {
                    Int::class -> it[1].toIntOrNull()
                    Float::class -> it[1].replace(",", "").toFloatOrNull()
                    else -> null
                }
            }
        }

        fun parse(parent: ProxyStream, url: HttpUrl, contents: String?): Playlist? {
            if (contents.isNullOrEmpty()) return null
            val lines = LinkedList(contents.split('\n').filter { !it.isEmpty() })

            // Parse header
            val header = lines.pop()
            if (header != "#EXTM3U") return null

            // Parse version
            val version = parseDelimited<Int>(lines.pop(), "#EXT-X-VERSION") ?: return null

            // Following the line, we can determine whether the playlist is a master or segment playlist
            return when (lines.peek().split(':').firstOrNull()) {
                "#EXT-X-MEDIA-SEQUENCE" -> parseSegmentPlaylist(version, parent, url, lines)
                "#EXT-X-STREAM-INF" -> parseMasterPlaylist(version, url, lines)
                else -> null
            }
        }

        private fun parseMasterPlaylist(version: Number, url: HttpUrl, lines: LinkedList<String>): MasterPlaylist? {
            // Read playlist qualities until no more lines
            val playlistDataMap = HashMap<PlaylistMetadata, String>()
            while (lines.isNotEmpty()) {
                val descriptor = lines.popOrNull()?.split(':')?.getOrNull(1) ?: continue
                val resource = lines.popOrNull() ?: continue

                // Parse quality options from descriptor
                val descriptions = descriptor.split(',')
                var programId: String? = null
                var resolution: String? = null
                var bandwidth: String? = null
                for (description in descriptions) {
                    val nameValuePair = description.split('=')

                    val name = nameValuePair.getOrNull(0)
                    when (name) {
                        "PROGRAM-ID" -> programId = nameValuePair.getOrNull(1)
                        "BANDWIDTH" -> bandwidth = nameValuePair.getOrNull(1)
                        "RESOLUTION" -> resolution = nameValuePair.getOrNull(1)
                        else -> println("Unknown property $name")
                    }
                }

                val metadata = PlaylistMetadata(programId ?: continue, bandwidth ?: continue, resolution ?: continue)
                playlistDataMap[metadata] = "${url.stub()}/$resource"
            }

            return MasterPlaylist(version, playlistDataMap)
        }

        private fun parseSegmentPlaylist(version: Number, parent: ProxyStream, url: HttpUrl,
                                         lines: LinkedList<String>): SegmentPlaylist? {
            // Skip media sequence and target duration
            (1..2).map { lines.pop() }

            // Collect segments until iterator is empty
            val stubbedUrl = url.stub()
            val segments = ArrayList<Segment>()

            while (lines.isNotEmpty()) {
                // Check if we need to prepend segment with discontinuity
                var discontinuity = false
                var next = lines.popOrNull() ?: continue
                if (next == "#EXT-X-DISCONTINUITY") {
                    discontinuity = true

                    next = lines.pop()
                }

                // After reading discontinuity, iterator might be empty
                if (lines.isEmpty()) continue

                val duration = parseDelimited<Float>(next, "#EXTINF") ?: continue
                val resource = lines.popOrNull() ?: continue
                val segmentName = "segment/${parent.addSegmentAlias(resource, stubbedUrl)}"
                val timestamp = resource.getTimestamp() ?: continue

                segments.add(Segment(url.toString(), segmentName, timestamp, duration,
                        discontinuity))
            }

            return SegmentPlaylist(version, segments)
        }
    }
}

private fun <E> LinkedList<E>.popOrNull() = if (this.isEmpty()) null else this.pop()

private fun String.getTimestamp(): Long? {
    val pattern = Pattern.compile("\\d+")
    val matcher = pattern.matcher(this)

    return if (matcher.find()) matcher.group().toLong() else null
}

private fun HttpUrl.stub(): String = with (this.toString()) {
    this.substring(0 until this.lastIndexOf('/'))
}

