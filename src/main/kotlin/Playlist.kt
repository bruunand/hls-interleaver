import okhttp3.HttpUrl
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class Segment(val source: String, val identifier: String, val time: Long, val duration: Number) : Comparable<Segment> {
    override fun compareTo(other: Segment): Int = time.compareTo(other.time)
}

class Playlist(var targetDuration: Number?, private val version: Number?, val segments: ArrayList<Segment>) {
    private val maxLength = 5

    fun synthesize(): String {
        val builder = StringBuilder()
        builder.appendln("#EXTM3U")
        builder.appendln("#EXT-X-VERSION:${this.version}")
        builder.appendln("#EXT-X-MEDIA-SEQUENCE:${this.segments.size}")

        val maxDuration = this.segments.map { it.duration.toDouble() }.max()?.let { Math.ceil(it).toInt() }
        builder.appendln("#EXT-X-TARGETDURATION:$maxDuration")

        var previousSource: String? = null
        for (entry in this.segments.takeLast(this.maxLength)) {
            if (previousSource != entry.source) {
                builder.appendln("#EXT-X-DISCONTINUITY")
                previousSource = entry.source
            }

            builder.appendln("#EXTINF:${entry.duration},")
            builder.appendln(entry.identifier)
        }

        return builder.toString()
    }

    companion object {
        private inline fun <reified T> parseDelimited(string: String, expectedHeader: String): Number? {
            string.split(':').let {
                if (it.size != 2) return null
                if (it[0] != expectedHeader) return null

                return when (T::class) {
                    Int::class -> it[1].toIntOrNull()
                    Float::class -> it[1].replace(",", "").toFloatOrNull()
                    else -> null
                }
            }
        }

        fun empty(version: Number = 3) = Playlist(0, version, ArrayList())

        fun parse(parent: ProxyStream, url: HttpUrl, contents: String?): Playlist? {
            if (contents.isNullOrEmpty()) return null
            val lineIterator = contents.split('\n').filter { !it.isEmpty() }.iterator()

            // Parse header
            val header = lineIterator.next()
            if (header != "#EXTM3U") return null

            // Parse version
            val version = parseDelimited<Int>(lineIterator.next(), "#EXT-X-VERSION") ?: return null

            // Skip media sequence
            lineIterator.next()

            // Parse target duration
            val targetDuration = parseDelimited<Int>(lineIterator.next(), "#EXT-X-TARGETDURATION") ?: return null

            // Collect segments until iteration is empty
            val stubbedUrl = url.stub()
            val segments = ArrayList<Segment>()
            while (lineIterator.hasNext()) {
                // Skip discontinuity
                val next = lineIterator.next()
                if (next == "#EXT-X-DISCONTINUITY") {
                    continue
                }

                val duration = parseDelimited<Float>(next, "#EXTINF")
                val resource = lineIterator.next()
                val segmentName = "${parent.name}/${parent.addSegmentAlias(resource, stubbedUrl)}"
                val timestamp = resource.getTimestamp()

                if (duration == null || timestamp == null) {
                    continue
                }

                segments.add(Segment(url.toString().split('-').first(), segmentName, timestamp, duration))
            }

            return Playlist(targetDuration, version, segments)
        }
    }
}

private fun String.getTimestamp(): Long? {
    val pattern = Pattern.compile("\\d+")
    val matcher = pattern.matcher(this)

    return if (matcher.find()) matcher.group().toLong() else null
}

private fun HttpUrl.stub(): String = with (this.toString()) {
    this.substring(0 until this.lastIndexOf('/'))
}

