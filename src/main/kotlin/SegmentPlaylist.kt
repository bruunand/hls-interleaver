class Segment(val source: String, val identifier: String, val time: Long, val duration: Number,
              val discontinuity: Boolean = false) : Comparable<Segment> {
    override fun compareTo(other: Segment): Int = time.compareTo(other.time)
}

class SegmentPlaylist(private val version: Number?, val segments: ArrayList<Segment>) : Playlist() {
    private val maxLength = 5

    override fun synthesize(): String {
        val builder = StringBuilder()
        builder.appendln("#EXTM3U")
        builder.appendln("#EXT-X-VERSION:${this.version}")
        builder.appendln("#EXT-X-MEDIA-SEQUENCE:${this.segments.size}")

        val maxDuration = this.segments.map { it.duration.toDouble() }.max()?.let { Math.ceil(it).toInt() } ?: 0
        builder.appendln("#EXT-X-TARGETDURATION:$maxDuration")

        var previousSource: String? = null
        for (entry in this.segments.takeLast(this.maxLength)) {
            if ((previousSource != null && previousSource != entry.source) || entry.discontinuity) {
                // TODO: Maybe optimal to mark the first ever segment with discontinuity
                builder.appendln("#EXT-X-DISCONTINUITY")
            }

            builder.appendln("#EXTINF:${entry.duration},")
            builder.appendln(entry.identifier)

            previousSource = entry.source
        }

        println(builder)

        return builder.toString()
    }

    // Add new segments to this playlists, i.e. ones with timestamps after the newest clip
    fun addNew(segments: List<Segment>) {
        val newestTimestamp = this.segments.max()?.time ?: 0
        val newestDuration = (this.segments.lastOrNull()?.duration as? Float)?.let {
            Math.round(500 * Math.floor(it.toDouble()) )
        } ?: 0

        // Add unseen segments that are newer than the newest segment
        val newSegments = segments.filter { it.time > newestTimestamp + newestDuration }
        println("${newSegments.size} new segments")
        this.segments.addAll(newSegments)
    }

    companion object {
        fun empty(version: Number = 3) = SegmentPlaylist(version, ArrayList())
    }
}