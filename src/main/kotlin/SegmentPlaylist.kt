class Segment(val source: String, val identifier: String, private val time: Long, val duration: Number,
              val discontinuity: Boolean = false) : Comparable<Segment> {
    override fun compareTo(other: Segment): Int = time.compareTo(other.time)
}

class SegmentPlaylist(private val version: Number?, private val segments: ArrayList<Segment>) : Playlist() {
    private val maxLength = 10

    override fun synthesize(): String {
        val builder = StringBuilder()
        builder.appendln("#EXTM3U")
        builder.appendln("#EXT-X-VERSION:${this.version}")
        builder.appendln("#EXT-X-MEDIA-SEQUENCE:${this.segments.size}")

        val maxDuration = this.segments.map { it.duration.toDouble() }.max()?.let { Math.ceil(it).toInt() } ?: 0
        builder.appendln("#EXT-X-TARGETDURATION:$maxDuration")

        var previousSource: String? = null
        for (entry in this.segments.takeLast(this.maxLength)) {
            if (previousSource != entry.source || entry.discontinuity) {
                builder.appendln("#EXT-X-DISCONTINUITY")
                previousSource = entry.source
            }

            builder.appendln("#EXTINF:${entry.duration},")
            builder.appendln(entry.identifier)
        }

        return builder.toString()
    }
}