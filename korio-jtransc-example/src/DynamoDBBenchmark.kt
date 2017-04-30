import com.soywiz.korio.async.*
import com.soywiz.korio.ext.amazon.dynamodb.DynamoDB
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpStats
import java.net.URL

fun main(args: Array<String>) = EventLoop {
	val eventLoop = this@EventLoop
	data class HelloTable(@DynamoDB.HashKey val myhash: String, @DynamoDB.RangeKey val myrange: String, val test: String)

	val dynamodb = DynamoDB(
			region = "eu-west-1",
			//endpoint = URL("http://127.0.0.1:3000"),
			endpoint = URL("http://127.0.0.1:8000"),
			//httpClientFactory = { HttpClient().delayed(500) },
			httpClientFactory = { HttpClient() },
			maxConnections = 50
	)
	val hello = dynamodb.typed<HelloTable>("hello")

	hello.deleteTableIfExists()
	hello.createIfNotExists()
	//val hello = dynamodb.typed<HelloTable>("hello")

	for (m in 0 until 1000) {
		println("Batch ($HttpStats):")

		val promises = arrayListOf<Promise<Unit>>()
		for (n in 0 until 50) {
			promises += spawn(eventLoop.coroutineContext) {
				try {
					val item = HelloTable("hash1", "range$n", "test")
					//println("Putting $item")
					hello.put(item)
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}
		}

		promises.await()

		for (item in hello.query { HelloTable::myhash eq "hash1" }) {
			println(item)
		}
	}
}