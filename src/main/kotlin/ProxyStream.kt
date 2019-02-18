import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

class ProxyStream(val name: String, private val endpoints: Array<String>) {
    private val segmentAlias: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val rand: Random = Random()
    val internalPlaylist: Playlist = Playlist.empty()

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

    private fun updatePlaylist() {
        val playlists = retrievePlaylists()
        println("Playlists: ${playlists.size}")

        if (!playlists.isEmpty()) {
            // Choose a random playlist to extract from
            val playlist = rand.choice(playlists)

            val newestTimestamp = internalPlaylist.segments.max()?.time ?: 0
            val newestSegmentDuration = internalPlaylist.segments.lastOrNull()?.duration?.let { Math.floor(it * 1000) } ?: 0

            // Add unseen segments that are newer than the newest segment
            internalPlaylist.segments.addAll(playlist.segments.filter { it.time >= newestTimestamp + newestSegmentDuration })
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