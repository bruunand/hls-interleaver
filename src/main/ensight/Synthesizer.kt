import java.util.*

object Synthetizer {
    fun synthesize(playlists: List<Playlist>): String {
        var random = Random().nextInt(60)
        random = if (random > 30) 0 else 1
        val list = playlists[random.toInt()]

        val builder = StringBuilder()
        builder.appendln("#EXTM3U")
        builder.appendln("#EXT-X-VERSION:${list.version}")
        builder.appendln("#EXT-X-MEDIA-SEQUENCE:${list.mediaSequence}}")
        builder.appendln("#EXT-X-TARGETDURATION:${list.targetDuration}")

        for (entry in list.segments.entries) {
            builder.appendln("#EXTINF:${entry.value},")
            builder.appendln(entry.key)
        }

        return builder.toString()
    }
}