import java.net.URL
import java.util.*

class Playlist(public val parent: ProxyStream, public val targetDuration: Number?, public val mediaSequence: Number?,
               public val version: Number?, public val segments: Map<String, Number?>) {
    companion object {
        private inline fun <reified T> parseDelimited(sequence: Iterator<String>, expectedHeader: String): Number? {
            sequence.next().split(':').let {
                if (it.size != 2) return null
                if (it[0] != expectedHeader) return null

                return when (T::class) {
                    Int::class -> it[1].toIntOrNull()
                    Float::class -> it[1].replace(",", "").toFloatOrNull()
                    else -> null
                }
            }
        }

        fun parse(parent: ProxyStream, url: String, contents: String?): Playlist? {
            println(contents)

            if (contents.isNullOrEmpty()) return null
            val lineIterator = contents.split('\n').filter { !it.isEmpty() }.iterator()

            // Parse header
            val header = lineIterator.next()
            if (header != "#EXTM3U") return null

            // Parse version
            val version = parseDelimited<Int>(lineIterator, "#EXT-X-VERSION") ?: return null

            // Parse media sequence
            val mediaSequence = parseDelimited<Int>(lineIterator, "#EXT-X-MEDIA-SEQUENCE") ?: return null

            // Parse target duration
            val targetDuration = parseDelimited<Int>(lineIterator, "#EXT-X-TARGETDURATION") ?: return null

            // Collect segments until iteration is empty
            val stubUrl = url.substring(0..url.lastIndexOf('/'))
            val segments = TreeMap<String, Number?>()
            while (lineIterator.hasNext()) {
                val length = parseDelimited<Float>(lineIterator, "#EXTINF")
                val segmentName = "${parent.name}/${parent.addSegmentAlias(lineIterator.next(), stubUrl)}"

                segments[segmentName] = length
            }

            return Playlist(parent, targetDuration, mediaSequence, version, segments)
        }
    }
}

