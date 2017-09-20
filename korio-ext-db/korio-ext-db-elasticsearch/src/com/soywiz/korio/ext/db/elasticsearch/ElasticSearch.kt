package com.soywiz.korio.ext.db.elasticsearch

import com.soywiz.korio.lang.KClass
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClientEndpoint
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.defaultHttpFactory
import com.soywiz.korio.net.http.rest.HttpRestClient
import com.soywiz.korio.net.http.rest.createRestClient
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.time.TimeSpan
import com.soywiz.korio.util.Dynamic

class ElasticSearch(
	rest: HttpRestClient = defaultHttpFactory.createRestClient("http://127.0.0.1:9200")
) : ElasticSearchResource(rest, "") {

	constructor(endpoint: String = "http://127.0.0.1:9200", httpFactory: HttpFactory = defaultHttpFactory) : this(httpFactory.createRestClient(endpoint))
	constructor(endpoint: HttpClientEndpoint) : this(endpoint.rest())

	operator fun get(index: String) = ElasticSearch.Index(this, index)
	operator fun get(index: String, type: String) = ElasticSearch.Type(this[index], type)

	inline fun <reified T : Any> typed(index: String, type: String) = this[index, type].typed<T>()

	data class PutResult(val id: String, val version: Long)

	class Index(
		es: ElasticSearch,
		name: String
	) : ElasticSearchResource(es.rest, name) {
		operator fun get(type: String) = ElasticSearch.Type(this, type)

		suspend fun exists(): Boolean = try {
			rest.head(resourcePath); true
		} catch (e: Http.HttpException) {
			false
		}

		suspend fun create(numberOfShards: Int = 8, numberOfReplicas: Int = 0) {
			val result = rest.put(resourcePath, mapOf(
				"settings" to mapOf(
					"index" to mapOf(
						"number_of_shards" to numberOfShards,
						"number_of_replicas" to numberOfReplicas
					)
				)
			))
			//println(result)
		}

		suspend fun ensure(numberOfShards: Int = 8, numberOfReplicas: Int = 0): Index {
			if (!exists()) {
				create(numberOfShards, numberOfReplicas)
			}
			return this
		}
	}

	open class CommonType(
		internal val esi: ElasticSearch.Index,
		val name: String
	) : ElasticSearchResource(esi.rest, "${esi.resourcePath}/$name") {

	}

	class Type(
		esi: ElasticSearch.Index,
		name: String
	) : CommonType(esi, name) {

		fun <T : Any> typed(clazz: KClass<T>): TypedType<T> = TypedType(this, clazz)
		inline fun <reified T : Any> typed(): TypedType<T> = TypedType(this, T::class)

		suspend fun delete(id: String): Boolean {
			return try {
				rest.delete("$resourcePath/$id")
				true
			} catch (e: Http.HttpException) {
				when (e.statusCode) {
					404 -> false
					else -> throw e
				}
			}
		}

		suspend fun get(id: String) = rest.get("$resourcePath/$id")

		suspend fun put(document: Any, id: String? = null): ElasticSearch.PutResult {
			//println("id:$id")
			val result = if (id != null) {
				rest.put("$resourcePath/$id", document)
			} else {
				rest.post(resourcePath, document)
			}
			//println(result)
			return ElasticSearch.PutResult(
				id = Dynamic.toString(Dynamic.getAnySync(result, "_id")),
				version = Dynamic.toString(Dynamic.getAnySync(result, "_version")).toLongOrNull() ?: 1L
			)
			//return ElasticSearch.PutResult(".")
		}
	}

	class TypedType<T : Any>(
		val type: Type,
		val clazz: KClass<T>
	) {
		// operator
		suspend fun get(id: String): T {
			val result = type.get(id)
			return Dynamic.dynamicCast(Dynamic.getAnySync(result, "_source") ?: mapOf<String, Any>(), clazz)!!
		}

		suspend fun getOrNull(id: String): T? = try {
			get(id)
		} catch (e: Http.HttpException) {
			when (e.statusCode) {
				404 -> null
				else -> throw e
			}
		}

		// operator
		suspend fun set(id: String, document: T) = type.put(document, id)
		suspend fun add(document: T): ElasticSearch.PutResult = type.put(document)
		suspend fun put(document: T, id: String? = null): ElasticSearch.PutResult = type.put(document, id)
		suspend fun delete(id: String) = type.delete(id)
		suspend fun search(query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query = { ElasticSearch.Query(ElasticSearch.QueryBuilder) }): SearchResult<T> {
			val result = type.search(query)
			return SearchResult(result.took, result.timedOut, result.results.map {
				Result<T>(it.index, it.type, it.id, it.score, Dynamic.dynamicCast(it.obj, clazz)!!)
			})
		}
		suspend fun searchList(query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query = { ElasticSearch.Query(ElasticSearch.QueryBuilder) }): List<T> = search(query).results.map { it.obj }

		// @TODO:
		suspend fun setMappingLanguage(lang: String): Unit {
			//type.put(document, id)
		}
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
			val map = LinkedHashMap<String, Any>()
			map["query"] = query
			if (use_dis_max != null) map["use_dis_max"] = use_dis_max
			if (fields != null) map["fields"] = fields
			return Query(mapOf("query_string" to map))
		}
	}
}

abstract class ElasticSearchResource(internal val rest: HttpRestClient, val resourcePath: String) {
	suspend fun search(query: ElasticSearch.QueryBuilder.() -> ElasticSearch.Query = { ElasticSearch.Query(ElasticSearch.QueryBuilder) }): ElasticSearch.SearchResult<Any> {
		val search = LinkedHashMap<String, Any>()

		val q = query(ElasticSearch.QueryBuilder)

		search["query"] = q.obj
		q.from?.let { search["from"] = it }
		q.size?.let { search["size"] = it }
		q.timeout?.let { search["timeout"] = "${it.milliseconds.toLong()}ms" }

		//println(search)

		val result = rest.post("$resourcePath/_search", search)

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
			ElasticSearch.Result<Any>(index, type, id, score, source ?: Any())
		}

		//println(result)

		return ElasticSearch.SearchResult(took, timedOut, results)
	}
}
