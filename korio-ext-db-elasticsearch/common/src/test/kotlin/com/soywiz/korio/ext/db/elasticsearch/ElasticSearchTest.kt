package com.soywiz.korio.ext.db.elasticsearch

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.net.http.FakeHttpClientEndpoint
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.serialization.ObjectMapper
import com.soywiz.korio.serialization.json.toJsonUntyped
import com.soywiz.korio.time.seconds
import kotlin.test.assertEquals

class ElasticSearchTest {
	data class Doc(val title: String, val body: String)

	val mapper = ObjectMapper()
	val endpoint = FakeHttpClientEndpoint()
	//val es = ElasticSearch(defaultHttpFactory.createClientEndpoint("http://127.0.0.1:9200"))
	val es = ElasticSearch(mapper, endpoint.rest())
	val posts = es.typed<Doc>("index-es", "posts")
	val posts2 = es["index-es", "posts"].typed<Doc>()

	init {
		mapper.registerType { Doc(it["title"].gen(), it["body"].gen()) }
		mapper.registerUntype<Doc> { lmapOf("title" to it.title, "body" to it.body) }
	}

	@kotlin.test.Test
	fun testPut() = syncTest {
		val myid = "myid"
		val myversion = 7L
		endpoint.addOkResponse(lmapOf("_id" to myid, "_version" to myversion).toJsonUntyped())
		endpoint.addOkResponse(lmapOf("_id" to myid, "_version" to myversion).toJsonUntyped())


		assertEquals(
			listOf("""POST:index-es/posts:{"title":"Hello","body":"World!"}"""),
			endpoint.capture {
				assertEquals(ElasticSearch.PutResult(myid, myversion), posts.put(Doc("Hello", "World!")))
			}
		)

		assertEquals(
			listOf("""PUT:index-es/posts/specific_id:{"title":"Hello","body":"World!"}"""),
			endpoint.capture {
				assertEquals(ElasticSearch.PutResult(myid, myversion), posts.put(Doc("Hello", "World!"), id = "specific_id"))
			}
		)
	}

	@kotlin.test.Test
	fun testSearch() = syncTest {
		val res1 = lmapOf(
			"took" to 77,
			"timed_out" to false,
			"hits" to lmapOf(
				"hits" to listOf(
					lmapOf("_index" to "myindex1", "_type" to "collection1", "_id" to "123", "_score" to 0.5, "_source" to lmapOf("title" to "Hello", "body" to "World")),
					lmapOf("_index" to "myindex2", "_type" to "collection2", "_id" to "456", "_score" to 0.25, "_source" to lmapOf("title" to "Other", "body" to "Document"))
				)
			)
		).toJsonUntyped()
		endpoint.addOkResponse(res1)
		endpoint.addOkResponse(res1)
		endpoint.addOkResponse(res1)

		assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"match_all":{}}}"""),
			endpoint.capture {
				val results = posts.search { match_all }

				assertEquals(ElasticSearch.SearchResult(took = 77, timedOut = false, results = listOf(
					ElasticSearch.Result("myindex1", "collection1", "123", 0.5, Doc("Hello", "World")),
					ElasticSearch.Result("myindex2", "collection2", "456", 0.25, Doc("Other", "Document"))
				)), results)
			}
		)

		assertEquals(
			listOf(Doc("Hello", "World"), Doc("Other", "Document")),
			posts.searchList { match_all }
		)

		assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"query_string":{"query":"yay!"}},"from":3,"size":10,"timeout":"2000ms"}"""),
			endpoint.capture {
				posts.search { query_string("yay!") LIMIT 10 SKIP 3 TIMEOUT 2.seconds }
			}
		)

		assertEquals(
			listOf("""POST:index-es/posts/_search:{"query":{"query_string":{"query":"yay!"}},"from":3,"size":10,"timeout":"2000ms"}"""),
			endpoint.capture {
				posts.search { query_string("yay!") SIZE 10 FROM 3 TIMEOUT 2.seconds }
			}
		)
	}

	@kotlin.test.Test
	fun testDelete() = syncTest {
		assertEquals(
			listOf("""DELETE:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				assertEquals(true, posts.delete("hello"))
			}
		)

		assertEquals(
			listOf("""DELETE:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")
				assertEquals(false, posts.delete("hello"))
			}
		)
	}

	@kotlin.test.Test
	fun testGetOrNull() = syncTest {
		assertEquals(
			listOf("""GET:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")

				assertEquals(null, posts.getOrNull("hello"))
			}
		)

		assertEquals(
			listOf("""GET:index-es/posts/hello:null"""),
			endpoint.capture {
				endpoint.addOkResponse(lmapOf(
					"_source" to lmapOf("title" to "hello", "body" to "world")
				).toJsonUntyped())

				assertEquals(Doc("hello", "world"), posts.getOrNull("hello"))
			}
		)
	}

	@kotlin.test.Test
	fun testIndexExists() = syncTest {
		assertEquals(
			listOf("""HEAD:index-es:null"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				assertEquals(true, es["index-es"].exists())
			}
		)
		assertEquals(
			listOf("""HEAD:index-es:null"""),
			endpoint.capture {
				endpoint.addNotFoundResponse("{}")
				assertEquals(false, es["index-es"].exists())
			}
		)
	}

	@kotlin.test.Test
	fun testIndexCreate() = syncTest {
		assertEquals(
			listOf("""PUT:index-es:{"settings":{"index":{"number_of_shards":8,"number_of_replicas":0}}}"""),
			endpoint.capture {
				endpoint.addOkResponse("{}")
				es["index-es"].create()
			}
		)
	}

	@kotlin.test.Test
	fun testIndexEnsure() = syncTest {
		assertEquals(
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

		assertEquals(
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