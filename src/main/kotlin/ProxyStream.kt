import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import io.github.rybalkinsd.kohttp.ext.httpGet
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

class ProxyStream(val name: String, private val endpoints: Array<String>) : Playlist() {
    private val rand: Random = Random()

    // Keep a mapping from distinct playlist metadata to the associated internal playlists7
    private val segmentLists: ConcurrentHashMap<PlaylistMetadata, SegmentPlaylist> = ConcurrentHashMap()

    // Furthermore, a mapping is used from string identifiers to playlist metadata
    private val metadataAliasMap: ConcurrentHashMap<String, PlaylistMetadata> = ConcurrentHashMap()

    // A mapping is used from string identifiers to full segment URLs on the origin server
    private val segmentAlias: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    init {
        fixedRateTimer(this.name, false, 0L, 2000) {
            updatePlaylist()
        }
    }

    private fun addSubplaylist(metadata: PlaylistMetadata, playlist: SegmentPlaylist) {
        segmentLists[metadata] = playlist
        metadataAliasMap[metadata.toString()] = metadata
    }

    fun getSubplaylist(metadataIdentifier: String) = segmentLists[metadataAliasMap[metadataIdentifier]]?.synthesize()
    
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

        for ((key, value) in metadataAliasMap) {
            builder.appendln("#EXT-X-STREAM-INF:PROGRAM-ID=${value.programId},BANDWIDTH=${value.bandwidth},RESOLUTION=${value.resolution}")
            builder.appendln("${this.name}/${key}")
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
        val time = (System.currentTimeMillis() / 1000) % 15
        println("Time: $time")
        val currentPlaylist = playlists[if (time >= 7) 0 else 1]
        when (currentPlaylist) {
            is MasterPlaylist -> {
                for ((key, value) in currentPlaylist.metadataMap) {
                    // If metadata is unseen, add the segment URL to an internal mapping
                    if (!segmentLists.containsKey(key)) {
                        addSubplaylist(key, SegmentPlaylist.empty())
                    }

                    // Read segments from this URL and add new segments
                    val playlist = this.retrieveSegmentPlaylist(value) ?: return
                    this.segmentLists[key]?.addNew(playlist.segments)
                }
            }
            else -> println("Unknown playlist type ${currentPlaylist::class}")
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
        // TODO: Find out if the async process is started as soon as it is called
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