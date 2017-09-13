package com.soywiz.korio.ext.db.elasticsearch

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.FakeHttpClientEndpoint
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.serialization.json.toJson
import com.soywiz.korio.time.seconds
import org.junit.Assert
import org.junit.Test

class ElasticSearchTest {
	data class Doc(val title: String, val body: String)

	val endpoint = FakeHttpClientEndpoint()
	//val es = ElasticSearch(defaultHttpFactory.createClientEndpoint("http://127.0.0.1:9200"))
	val es = ElasticSearch(endpoint.rest())
	val posts = es.typed<Doc>("index-es", "posts")
	val posts2 = es["index-es", "posts"].typed<Doc>()

	@Test
	fun testPut() = syncTest {
		val myid = "myid"
		val myversion = 7L
		endpoint.addOkResponse(mapOf("_id" to myid, "_version" to myversion).toJson())
		endpoint.addOkResponse(mapOf("_id" to myid, "_version" to myversion).toJson())


		Assert.assertEquals(
			listOf("""POST:index-es/posts:{"title":"Hello","body":"World!"}"""),
			endpoint.capture {
				Assert.assertEquals(ElasticSearch.PutResult(myid, myversion), posts.put(Doc("Hello", "World!")))
			}
		)

		Assert.assertEquals(
			listOf("""PUT:index-es/posts/specific_id:{"title":"Hello","body":"World!"}"""),
			endpoint.capture {
				Assert.assertEquals(ElasticSearch.PutResult(myid, myversion), posts.put(Doc("Hello", "World!"), id = "specific_id"))
			}
		)
	}

	@Test
	fun testSearch() = syncTest {
		val res1 = mapOf(
			"took" to 77,
			"timed_out" to false,
			"hits" to mapOf(
				"hits" to listOf(
					mapOf("_index" to "myindex1", "_type" to "collection1", "_id" to "123", "_score" to 0.5, "_source" to mapOf("title" to "Hello", "body" to "World")),
					mapOf("_index" to "myindex2", "_type" to "collection2", "_id" to "456", "_score" to 0.25, "_source" to mapOf("title" to "Other", "body" to "Document"))
				)
			)
		).toJson()
		endpoint.addOkResponse(res1)
		endpoint.addOkResponse(res1)
		endpoint.addOkResponse(res1)

		Assert.assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"match_all":{}}}"""),
			endpoint.capture {
				val results = posts.search { match_all }

				Assert.assertEquals(ElasticSearch.SearchResult(took = 77, timedOut = false, results = listOf(
					ElasticSearch.Result("myindex1", "collection1", "123", 0.5, Doc("Hello", "World")),
					ElasticSearch.Result("myindex2", "collection2", "456", 0.25, Doc("Other", "Document"))
				)), results)
			}
		)

		Assert.assertEquals(
			listOf(Doc("Hello", "World"), Doc("Other", "Document")),
			posts.searchList { match_all }
		)

		Assert.assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"query_string":{"query":"yay!"}},"from":3,"size":10,"timeout":"2000ms"}"""),
			endpoint.capture {
				posts.search { query_string("yay!") LIMIT 10 SKIP 3 TIMEOUT 2.seconds }
			}
		)

		Assert.assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"query_string":{"query":"yay!"}},"from":3,"size":10,"timeout":"2000ms"}"""),
			endpoint.capture {
				posts.search { query_string("yay!") SIZE 10 FROM 3 TIMEOUT 2.seconds }
			}
		)
	}

	@Test
	fun testDelete() = syncTest {
		Assert.assertEquals(
			listOf("""DELETE:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				Assert.assertEquals(true, posts.delete("hello"))
			}
		)

		Assert.assertEquals(
			listOf("""DELETE:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")
				Assert.assertEquals(false, posts.delete("hello"))
			}
		)
	}

	@Test
	fun testGetOrNull() = syncTest {
		Assert.assertEquals(
			listOf("""GET:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")

				Assert.assertEquals(null, posts.getOrNull("hello"))
			}
		)

		Assert.assertEquals(
			listOf("""GET:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addOkResponse(mapOf(
					"_source" to mapOf("title" to "hello", "body" to "world")
				).toJson())

				Assert.assertEquals(Doc("hello", "world"), posts.getOrNull("hello"))
			}
		)
	}

	@Test
	fun testIndexExists() = syncTest {
		Assert.assertEquals(
			listOf("""HEAD:index-es:null"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				Assert.assertEquals(true, es["index-es"].exists())
			}
		)
		Assert.assertEquals(
			listOf("""HEAD:index-es:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")
				Assert.assertEquals(false, es["index-es"].exists())
			}
		)
	}

	@Test
	fun testIndexCreate() = syncTest {
		Assert.assertEquals(
			listOf("""PUT:index-es:{"settings":{"index":{"number_of_shards":8,"number_of_replicas":0}}}"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				es["index-es"].create()
			}
		)
	}

	@Test
	fun testIndexEnsure() = syncTest {
		Assert.assertEquals(
			listOf(
				"""HEAD:index-es:null""",
				"""PUT:index-es:{"settings":{"index":{"number_of_shards":8,"number_of_replicas":0}}}"""
			),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")
				endpoint.addOkResponse("{}")
				es["index-es"].ensure()
			}
		)

		Assert.assertEquals(
			listOf(
				"""HEAD:index-es:null"""
			),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				endpoint.addOkResponse("{}")
				es["index-es"].ensure()
			}
		)
	}

}