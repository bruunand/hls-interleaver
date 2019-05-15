import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.rybalkinsd.kohttp.ext.httpGet
import java.util.concurrent.TimeUnit

val segmentCache: LoadingCache<String, ByteArray?> = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build { it.httpGet().use { r -> r.body()?.bytes() } }