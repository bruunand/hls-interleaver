import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

// Small ad-hoc test which shows the result of retrieving the same page many times in parallel
fun main(args: Array<String>)  {
    val deferred = (1..1000).map {
        GlobalScope.async {
            Segments.retrieve("http://anderslangballe.dk")
        }
    }

    runBlocking {
        deferred.forEach { println(it.await()) }
    }

    println("Done")
}