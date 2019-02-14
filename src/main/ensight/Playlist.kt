import java.net.URL
import java.util.*
import java.util.regex.Pattern

class Segment(val identifier: String, val time: Long, val duration: Number) : Comparable<Segment> {
    override fun compareTo(other: Segment): Int = time.compareTo(other.time)
}

class Playlist(val parent: ProxyStream, val targetDuration: Number?, val mediaSequence: Number?, val version: Number?,
               val segments: Map<String, Segment>) {
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
            val segments = TreeMap<String, Segment>()
            while (lineIterator.hasNext()) {
                val length = parseDelimited<Float>(lineIterator, "#EXTINF")
                val resource = lineIterator.next()
                val segmentName = "${parent.name}/${parent.addSegmentAlias(resource, stubUrl)}"
                val pattern = Pattern.compile("-?\\d+")
                println(pattern.matcher(resource).group())

                // segments[segmentName] = length
            }

            return Playlist(parent, targetDuration, mediaSequence, version, segments)
        }
    }
}

