package com.soywiz.korio.vertx

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*


object NettyHttpFactory {
	@JvmStatic
	fun main(args: Array<String>) {
		val server = ServerBootstrap()
		server.childHandler(object : ChannelInitializer<SocketChannel>() {
			override fun initChannel(ch: SocketChannel) {
				val p = ch.pipeline()
				p.addLast(HttpRequestDecoder())
				p.addLast(HttpResponseEncoder())
			}
		})
		server.bind(7171)
	}
}

class NettyHttpServer {
	private var channel: ChannelFuture? = null
	private val masterGroup: EventLoopGroup
	private val slaveGroup: EventLoopGroup

	init {
		masterGroup = NioEventLoopGroup()
		slaveGroup = NioEventLoopGroup()
	}

	fun start() // #1
	{
		Runtime.getRuntime().addShutdownHook(object : Thread() {
			override fun run() {
				shutdown()
			}
		})

		try {
			// #3
			val bootstrap = ServerBootstrap()
				.group(masterGroup, slaveGroup)
				.channel(NioServerSocketChannel::class.java)
				.childHandler(object : ChannelInitializer<SocketChannel>() // #4
				{
					@Throws(Exception::class)
					public override fun initChannel(ch: SocketChannel) {
						val pipeline = ch.pipeline()
						pipeline.addLast("codec", HttpServerCodec())
						pipeline.addLast("aggregator",
							HttpObjectAggregator(512 * 1024))
						pipeline.addLast("request",
							object : ChannelInboundHandlerAdapter() // #5
							{
								@Throws(Exception::class)
								override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
									if (msg is FullHttpRequest) {

										val responseMessage = "Hello from Netty!"

										val response = DefaultFullHttpResponse(
											HttpVersion.HTTP_1_1,
											HttpResponseStatus.OK,
											Unpooled.copiedBuffer(responseMessage.toByteArray())
										)

										if (HttpUtil.isKeepAlive(msg)) {
											response.headers().set(
												HttpHeaderNames.CONNECTION,
												HttpHeaderValues.KEEP_ALIVE
											)
										}
										response.headers().set(HttpHeaderNames.CONTENT_TYPE,
											"text/plain")
										response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
											responseMessage.length)

										ctx.writeAndFlush(response)
									} else {
										super.channelRead(ctx, msg)
									}
								}

								@Throws(Exception::class)
								override fun channelReadComplete(ctx: ChannelHandlerContext) {
									ctx.flush()
								}

								@Throws(Exception::class)
								override fun exceptionCaught(ctx: ChannelHandlerContext,
															 cause: Throwable) {
									ctx.writeAndFlush(DefaultFullHttpResponse(
										HttpVersion.HTTP_1_1,
										HttpResponseStatus.INTERNAL_SERVER_ERROR,
										Unpooled.copiedBuffer(cause.message!!.toByteArray())
									))
								}
							})
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
			channel = bootstrap.bind(8080).sync()
		} catch (e: InterruptedException) {
		}

	}

	fun shutdown() // #2
	{
		slaveGroup.shutdownGracefully()
		masterGroup.shutdownGracefully()

		try {
			channel!!.channel().closeFuture().sync()
		} catch (e: InterruptedException) {
		}

	}

	companion object {

		@JvmStatic
		fun main(args: Array<String>) {
			println("Starting")
			NettyHttpServer().start()
			println("**")
		}
	}
}
