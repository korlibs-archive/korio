package com.soywiz.korio.ext.db.elasticsearch

import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.defaultHttpFactory
import com.soywiz.korio.net.http.rest.HttpRestClient
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.time.TimeSpan
import com.soywiz.korio.util.Dynamic

class ElasticSearch private constructor(
	private val baseUrl: String,
	private val httpFactory: HttpFactory,
	rest: HttpRestClient
) : ElasticSearchResource(rest, baseUrl) {

	constructor(baseUrl: String = "http://127.0.0.1:9200", httpFactory: HttpFactory = defaultHttpFactory) : this(baseUrl, httpFactory, httpFactory.createClient().rest())

	operator fun get(index: String) = ElasticSearch.Index(this, index)
	operator fun get(index: String, type: String) = ElasticSearch.Type(this[index], type)

	data class PutResult(val id: String, val version: Long)

	class Index(
		es: ElasticSearch,
		name: String
	) : ElasticSearchResource(es.rest, "${es.baseUrl}/$name") {
		operator fun get(type: String) = ElasticSearch.Type(this, type)

		suspend fun exists(): Boolean = try {
			rest.head(resourceUrl); true
		} catch (e: Http.HttpException) {
			false
		}

		suspend fun create(numberOfShards: Int = 8, numberOfReplicas: Int = 0) {
			val result = rest.put(resourceUrl, mapOf(
				"settings" to mapOf(
					"index" to mapOf(
						"number_of_shards" to numberOfShards,
						"number_of_replicas" to numberOfReplicas
					)
				)
			))
			println(result)
		}

		suspend fun ensure(numberOfShards: Int = 8, numberOfReplicas: Int = 0): Index {
			if (!exists()) {
				create(numberOfShards, numberOfReplicas)
			}
			return this
		}
	}

	class Type(
		internal val esi: ElasticSearch.Index,
		val name: String
	) : ElasticSearchResource(esi.rest, "${esi.resourceUrl}/$name") {

		suspend fun put(document: Any, id: String? = null): ElasticSearch.PutResult {
			//println("id:$id")
			val result = if (id != null) {
				rest.put("$resourceUrl/$id", document)
			} else {
				rest.post(resourceUrl, document)
			}
			//println(result)
			return ElasticSearch.PutResult(
				id = Dynamic.toString(Dynamic.getAnySync(result, "_id")),
				version = Dynamic.toString(Dynamic.getAnySync(result, "_version")).toLongOrNull() ?: 1L
			)
			//return ElasticSearch.PutResult(".")
		}

		suspend fun <T : Any> searchTyped(clazz: Class<T>, query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query = { ElasticSearch.Query(ElasticSearch.QueryBuilder) }): SearchResult<T> {
			val result = search(query)
			return SearchResult(result.took, result.timedOut, result.results.map {
				Result(it.index, it.type, it.id, it.score, Dynamic.dynamicCast(it.obj, clazz)!!)
			})
		}

		suspend inline fun <reified T : Any> searchTyped(noinline query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query): SearchResult<T> = searchTyped(T::class.java, query)
		suspend inline fun <reified T : Any> searchTyped(): SearchResult<T> = searchTyped(T::class.java)
	}

	data class SearchResult<T>(
		val took: Int,
		val timedOut: Boolean,
		val results: List<Result<T>>
	)

	data class Result<T>(val index: String, val type: String, val id: String, val score: Double, val obj: T)

	data class Query(val obj: Any, val from: Int? = null, val size: Int? = null, val timeout: TimeSpan? = null) {
		infix fun TIMEOUT(time: TimeSpan): Query = this.copy(timeout = time)

		infix fun FROM(count: Int): Query = this.copy(from = count)
		infix fun SKIP(count: Int): Query = this.copy(from = count)

		infix fun SIZE(count: Int): Query = this.copy(size = count)
		infix fun LIMIT(count: Int): Query = this.copy(size = count)
	}

	object QueryBuilder {
		val match_all get() = Query(mapOf("match_all" to mapOf<String, String>()))

		fun query_string(
			query: String,
			use_dis_max: Boolean? = null,
			fields: List<String>? = null
		): Query {
			val map = HashMap<String, Any>()
			map["query"] = query
			if (use_dis_max != null) map["use_dis_max"] = use_dis_max
			if (fields != null) map["fields"] = fields
			return Query(mapOf("query_string" to map))
		}
	}
}

abstract class ElasticSearchResource(internal val rest: HttpRestClient, val resourceUrl: String) {
	suspend fun search(query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query = { ElasticSearch.Query(ElasticSearch.QueryBuilder) }): ElasticSearch.SearchResult<Any> {
		val search = HashMap<String, Any>()

		val q = query(ElasticSearch.QueryBuilder)

		search["query"] = q.obj
		q.from?.let { search["from"] = it }
		q.size?.let { search["size"] = it }
		q.timeout?.let { search["timeout"] = "${it.milliseconds.toLong()}ms" }

		//println(search)

		val result = rest.post("$resourceUrl/_search", search)

		val took = Dynamic.toInt(Dynamic.getAnySync(result, "took"))
		val timedOut = Dynamic.toBool(Dynamic.getAnySync(result, "timed_out"))
		val hits = Dynamic.getAnySync(result, "hits")
		val hitsHits = Dynamic.toList(Dynamic.getAnySync(hits, "hits"))

		val results = hitsHits.map { hit ->
			val index = Dynamic.toString(Dynamic.getAnySync(hit, "_index"))
			val type = Dynamic.toString(Dynamic.getAnySync(hit, "_type"))
			val id = Dynamic.toString(Dynamic.getAnySync(hit, "_id"))
			val score = Dynamic.toDouble(Dynamic.getAnySync(hit, "_score"))
			val source = Dynamic.getAnySync(hit, "_source")
			ElasticSearch.Result<Any>(index, type, id, score, source ?: Object())
		}

		println(result)

		return ElasticSearch.SearchResult(took, timedOut, results)
	}
}
