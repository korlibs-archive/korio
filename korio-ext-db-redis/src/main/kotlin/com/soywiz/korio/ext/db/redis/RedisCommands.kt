package com.soywiz.korio.ext.db.redis

// @TODO: Missing commands

suspend fun RedisCommand.append(key: String, value: String) = commandString("append", key, value)
suspend fun RedisCommand.auth(password: String) = commandString("auth", password)
suspend fun RedisCommand.bgrewriteaof() = commandString("bgrewriteaof")
suspend fun RedisCommand.bgsave() = commandString("bgsave")
suspend fun RedisCommand.bitcount(key: String) = commandString("bitcount", key)
suspend fun RedisCommand.bitcount(key: String, start: Int, end: Int) = commandString("bitcount", key, "$start", "$end")

suspend fun RedisCommand.set(key: String, value: String) = commandString("set", key, value)
suspend fun RedisCommand.get(key: String) = commandString("get", key)
suspend fun RedisCommand.del(vararg keys: String) = commandString("del", *keys)
suspend fun RedisCommand.echo(msg: String) = commandString("echo", msg)


suspend fun RedisCommand.hset(key: String, member: String, value: String): Long = commandLong("hset", key, member, value)

suspend fun RedisCommand.hget(key: String, member: String): String? = commandString("hget", key, member)
suspend fun RedisCommand.hincrby(key: String, member: String, increment: Long): Long = commandLong("hincrby", key, member, "$increment")
suspend fun RedisCommand.zadd(key: String, vararg scores: Pair<String, Double>): Long {
	val args = arrayListOf<Any?>()
	for (score in scores) {
		args += score.second
		args += score.first
	}
	return commandLong("zadd", key, *args.toTypedArray())
}

suspend fun RedisCommand.zadd(key: String, member: String, score: Double): Long = commandLong("zadd", key, score, member)
suspend fun RedisCommand.sadd(key: String, member: String): Long = commandLong("sadd", key, member)
suspend fun RedisCommand.smembers(key: String): List<String> = commandArray("smembers", key)

suspend fun RedisCommand.zincrby(key: String, member: String, score: Double) = commandString("zincrby", key, score, member)!!
suspend fun RedisCommand.zcard(key: String): Long = commandLong("zcard", key)
suspend fun RedisCommand.zrevrank(key: String, member: String): Long = commandLong("zrevrank", key, member)
suspend fun RedisCommand.zscore(key: String, member: String): Long = commandLong("zscore", key, member)

private fun List<Any?>.listOfPairsToMap(): Map<String, String> {
	val list = this
	return (0 until list.size / 2).map { ("" + list[it * 2 + 0]) to ("" + list[it * 2 + 1]) }.toMap()
}

suspend fun RedisCommand.hgetall(key: String): Map<String, String> {
	return commandArray("hgetall", key).listOfPairsToMap()
}

suspend fun RedisCommand.zrevrange(key: String, start: Long, stop: Long): Map<String, Double> {
	return commandArray("zrevrange", key, start, stop, "WITHSCORES").listOfPairsToMap().mapValues { "${it.value}".toDouble() }
}
