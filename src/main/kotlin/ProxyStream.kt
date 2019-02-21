import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import io.github.rybalkinsd.kohttp.ext.httpGet
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

class ProxyStream(val name: String, private val endpoints: Array<String>) : Playlist() {
    // Keep a mapping from distinct playlist metadata to the associated internal playlists
    private val segmentLists: ConcurrentHashMap<PlaylistMetadata, SegmentPlaylist> = ConcurrentHashMap()
    private val segmentAlias: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val rand: Random = Random()

    init {
        fixedRateTimer(this.name, false, 0L, 1000) {
            updatePlaylist()
        }
    }

    fun addSegmentAlias(source: String, stubUrl: String): String {
        val fullUrl = "$stubUrl/$source"
        val hashCode = fullUrl.hashCode().toString()
        segmentAlias[hashCode] = fullUrl

        return hashCode
    }

    fun getSegmentURL(segment: String) = this.segmentAlias.getOrDefault(segment, null)

    override fun synthesize(): String {
        val builder = StringBuilder()
        builder.appendln("#EXTM3U")
        builder.appendln("#EXT-X-VERSION:3")

        for (key in segmentLists.keys()) {
            builder.appendln("#EXT-X-STREAM-INF:PROGRAM-ID=${key.programId},BANDWIDTH=${key.bandwidth},RESOLUTION=${key.resolution}")
            // TODO: Append some alias
        }

        return builder.toString()
    }

    private fun updatePlaylist() {
        val playlists = retrievePlaylists()

        // Do nothing if there are no playlists
        if (playlists.isEmpty()) {
            return
        }

        // Use the currently selected playlist
        val currentPlaylist = rand.choice(playlists)
        when (currentPlaylist) {
            is MasterPlaylist -> {
                for ((key, value) in currentPlaylist.metadataMap) {
                    // If metadata is unseen, add the segment URL to an internal mapping
                    if (key !in segmentLists) {
                        this.segmentLists[key] = SegmentPlaylist.empty()
                    }

                    // Read segments from this URL and add new segments
                    val playlist = this.retrieveSegmentPlaylist(value) ?: return
                    this.segmentLists[key]?.addNew(playlist.segments)
                }
            }
            is SegmentPlaylist -> {
                // TODO: Legacy support
            }
            else -> println("Unknown playlist type $currentPlaylist")
        }
    }

    private fun retrieveSegmentPlaylist(url: String): SegmentPlaylist? {
        val response = url.httpGet()

        try {
            return Playlist.parse(this, response.request().url(), response.body()?.string() ?: return null) as? SegmentPlaylist
        } finally {
            response.body()?.close()
        }
    }

    private fun retrieveRemotes() = runBlocking {
        val futures = endpoints.map { it.asyncHttpGet() }
        futures.map { it.await() }
    }

    private fun retrievePlaylists(): List<Playlist> {
        val remotes = retrieveRemotes()
        return remotes.mapNotNull {
            try {
                when (it.code()) {
                    200 -> Playlist.parse(this, it.request().url(), it.body()?.string())
                    else -> null
                }
            } finally {
                it.body()?.close()
            }
        }
    }
}

private fun <T> Random.choice(playlists: List<T>) = playlists[nextInt(playlists.size)]