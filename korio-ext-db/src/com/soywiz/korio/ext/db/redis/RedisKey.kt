package com.soywiz.korio.ext.db.redis

// @TODO: Missing commands

fun Redis.key(key: String) = RedisKey(this, key)

class RedisKey(val client: Redis, val key: String)

suspend fun RedisKey.hset(member: String, value: String): Long = client.hset(key, member, value)

suspend fun RedisKey.hget(member: String): String? = client.hget(key, member)
suspend fun RedisKey.hincrby(member: String, increment: Long): Long = client.hincrby(key, member, increment)
suspend fun RedisKey.zaddMany(vararg scores: Pair<String, Double>): Long = client.zadd(key, *scores)
suspend fun RedisKey.zadd(member: String, score: Double): Long = client.zadd(key, member, score)
suspend fun RedisKey.sadd(member: String): Long = client.sadd(key, member)
suspend fun RedisKey.smembers(): List<String> = client.smembers(key)
suspend fun RedisKey.zincrby(member: String, score: Double): String = client.zincrby(key, member, score)
suspend fun RedisKey.zcard(): Long = client.zcard(key)
suspend fun RedisKey.zrevrank(member: String): Long = client.zrevrank(key, member)
suspend fun RedisKey.zscore(member: String): Long = client.zscore(key, member)
suspend fun RedisKey.hgetall(): Map<String, String> = client.hgetall(key)
suspend fun RedisKey.zrevrange(start: Long, stop: Long): Map<String, Double> = client.zrevrange(key, start, stop)
suspend fun RedisKey.del() = client.del(key)
