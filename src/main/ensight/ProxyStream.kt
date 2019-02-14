import io.github.rybalkinsd.kohttp.ext.asyncHttpGet
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.fixedRateTimer

class ProxyStream(val name: String, private val endpoints: Array<String>) {
    private val segmentAliasMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    init {
        playlistRetriever()
    }

    fun addSegmentAlias(source: String, stubUrl: String): String {
        segmentAliasMap[source] = "$stubUrl/$source"

        // Returns the alias to be used for retrieval
        return source
    }

    fun getSegmentURL(segment: String) = this.segmentAliasMap.getOrDefault(segment, null)

    private fun playlistRetriever() {
        fixedRateTimer(this.name, false, 0L, 1000){
            val playlists = retrievePlaylists()

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
                200 -> Playlist.parse(this, it.request().url().toString(), it.body()?.string())
                else -> null
            }
        }
    }
}