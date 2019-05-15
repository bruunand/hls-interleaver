import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Segments {
    suspend fun retrieve(segmentUrl: String): ByteArray? = lock.withLock {
        lookupMap[segmentUrl] ?: let {
            val deferred = requestAsync(segmentUrl)
            lookupMap[segmentUrl] = deferred
            deferred
        }
    }.await()

    private fun requestAsync(segmentUrl: String) = GlobalScope.async {
        println("Request segment $segmentUrl")
        println("Size: ${lookupMap.size}")
        khttp.get(segmentUrl).content
    }

    // Map segment URLs to future HTTP retrievals
    private val lookupMap = HashMap<String, Deferred<ByteArray?>>(5)

    // Lock to ensure that only one deferred response can be created at once
    private val lock = ReentrantLock()
}