import io.github.rybalkinsd.kohttp.ext.httpGet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Segments {
    suspend fun retrieve(segmentUrl: String): ByteArray? {
        return (cache[segmentUrl] as? ByteArray) ?: let {
            lock.withLock {
                lookupMap[segmentUrl] ?: let {
                    val deferred = request(segmentUrl)
                    lookupMap[segmentUrl] = deferred
                    deferred
                }
            }.await()
        }
    }

    private fun request(segmentUrl: String) = GlobalScope.async {
        println("Request segment $segmentUrl")
        val retrieval = segmentUrl.httpGet()

        try {
            retrieval.body()?.bytes()?.let {
                cache[segmentUrl] = it
                it
            }
        } finally {
            retrieval.body()?.close()
        }
    }

    // Internal FIFO cache
    private val cache = FIFOCache(capacity = 5)

    // Map segment URLs to future HTTP retrievals
    private val lookupMap = ConcurrentHashMap<String, Deferred<ByteArray?>>()

    // Lock to ensure that only one deferred response can be created at once
    private val lock = ReentrantLock()
}