package com.soywiz.korio.ext.db.elasticsearch

import com.soywiz.korio.async.syncTest
import org.junit.Ignore
import org.junit.Test

class ElasticSearchTest {
	@Test
	@Ignore
	fun name() = syncTest {
		val es = ElasticSearch()
		val posts = es["index-es"]["posts"]
		//println(es["index-es"].exists())
		//println(es["index-es"].ensure())
		/*
		val result = posts.put(mapOf(
			"title" to "Hello",
			"body" to "World!"
		))
		*/

		//for (n in 0 until 1000) {
		//	println("Inserting $n...")
		//	posts.put(mapOf(
		//		"title" to "Demo$n",
		//		"body" to "Demo$n test"
		//	))
		//}

		//println(result)
		data class Doc(val title: String, val body: String)

		//val results = posts.searchTyped<Doc> { match_all }
		//val results = posts.searchTyped<Doc> { query_string("hell*", fields = listOf("title")) }
		val results = posts.searchTyped<Doc> { query_string("test") }
		println(results)
		for (result in results.results) {
			println(result)
		}
	}
}