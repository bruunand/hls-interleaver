import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

class ProxyStream(val name: String, private val endpoints: Array<String>) {
    private val segmentAlias: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    val internalPlaylist: Playlist = Playlist.empty()

    init {
        playlistRetriever()
    }

    fun addSegmentAlias(source: String, stubUrl: String): String {
        segmentAlias[source] = "$stubUrl/$source"

        // Returns the alias to be used for retrieval
        return source
    }

    fun getSegmentURL(segment: String) = this.segmentAlias.getOrDefault(segment, null)

    private fun playlistRetriever() {
        fixedRateTimer(this.name, false, 0L, 1000){
            val playlists = retrievePlaylists()

            if (!playlists.isEmpty()) {
                // Choose a random playlist to extract from
                val playlistId = Random().nextInt(playlists.size)
                println("Playlist: $playlistId")
                val playlist = playlists[playlistId]

                // Use whatever target duration the current playlist is using
                internalPlaylist.targetDuration = playlist.targetDuration

                // Add unseen segments that are newer than the newest segment
                val newestTimestamp = internalPlaylist.segments.max()?.time ?: 0

                internalPlaylist.segments.addAll(playlist.segments.filter { it.time > newestTimestamp })
            }
        }
    }

    private fun retrieveRemotes() = runBlocking {
        val futures = endpoints.map { it.asyncHttpGet() }
        futures.map { it.await() }
    }

    private fun retrievePlaylists(): List<Playlist> {
        val remotes = retrieveRemotes()
        return remotes.mapNotNull {
            when (it.code()) {
                200 -> Playlist.parse(this, it.request().url(), it.body()?.string())
                else -> null
            }
        }
    }
}