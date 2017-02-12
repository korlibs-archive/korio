import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.ext.db.redis.hset
import com.soywiz.korio.net.AsyncClient
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) = EventLoop.main {
	try {
		val redis = Redis(maxConnections = 100)
		val startedCount = AtomicInteger(0)
		val completedCount = AtomicInteger(0)
		val start = System.currentTimeMillis()
		EventLoop.setIntervalImmediate(500) {
			val elapsed = System.currentTimeMillis() - start
			println("$elapsed: $completedCount/$startedCount : ${redis.stats} : ${AsyncClient.Stats}")
		}
		//for (n in 0 until 1000000) {
		for (n in 0 until 100000) {
			spawnAndForget {
				try {
					startedCount.incrementAndGet()
					redis.hset("MYKEY1", "world", "value")
				} catch (t: Throwable) {
					t.printStackTrace()
				} finally {
					completedCount.incrementAndGet()
				}
				Unit
			}
		}
	} catch (t: Throwable) {
		println(t)
	}
}