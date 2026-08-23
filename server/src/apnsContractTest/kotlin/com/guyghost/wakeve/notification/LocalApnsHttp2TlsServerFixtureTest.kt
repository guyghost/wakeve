package com.guyghost.wakeve.notification

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.handler.codec.http2.Http2StreamFrame
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SupportedCipherSuiteFilter
import io.netty.handler.ssl.util.SelfSignedCertificate
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Loopback APNs endpoint used only by the isolated 5.x test source set.
 *
 * It accepts real TLS with ALPN h2, decodes one HTTP/2 stream, records the exact wire request,
 * and returns the supplied APNs response. It has no production configuration or network route.
 */
class LocalApnsHttp2TlsServer private constructor(
    private val boss: NioEventLoopGroup,
    private val workers: NioEventLoopGroup,
    private val channel: Channel,
    val trustAnchor: SelfSignedCertificate,
    private val requests: LinkedBlockingQueue<CapturedApnsHttp2Request>
) : AutoCloseable {
    val endpoint: InetSocketAddress get() = channel.localAddress() as InetSocketAddress

    fun awaitRequest(): CapturedApnsHttp2Request = checkNotNull(requests.poll(5, TimeUnit.SECONDS)) { "APNs loopback did not receive a request" }

    fun awaitRequests(count: Int): List<CapturedApnsHttp2Request> = List(count) { awaitRequest() }

    override fun close() {
        channel.close().syncUninterruptibly()
        workers.shutdownGracefully().syncUninterruptibly()
        boss.shutdownGracefully().syncUninterruptibly()
        trustAnchor.delete()
    }

    companion object {
        fun start(
            response: APNsHttp2Response,
            fault: LocalApnsHttp2TlsFault = LocalApnsHttp2TlsFault.RESPOND,
            supportedTlsProtocols: Array<String> = arrayOf("TLSv1.2", "TLSv1.3")
        ): LocalApnsHttp2TlsServer = start(listOf(response), fault, supportedTlsProtocols)

        fun start(
            responses: List<APNsHttp2Response>,
            fault: LocalApnsHttp2TlsFault = LocalApnsHttp2TlsFault.RESPOND,
            supportedTlsProtocols: Array<String> = arrayOf("TLSv1.2", "TLSv1.3")
        ): LocalApnsHttp2TlsServer {
            require(responses.isNotEmpty())
            val certificate = SelfSignedCertificate("localhost")
            val observedRequests = LinkedBlockingQueue<CapturedApnsHttp2Request>()
            val scriptedResponses = ArrayDeque(responses)
            val boss = NioEventLoopGroup(1)
            val workers = NioEventLoopGroup(1)
            val context = serverContext(certificate, supportedTlsProtocols)
            val channel = ServerBootstrap()
                .group(boss, workers)
                .channel(NioServerSocketChannel::class.java)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socket: SocketChannel) {
                        socket.pipeline().addLast(context.newHandler(socket.alloc()))
                        socket.pipeline().addLast(ServerAlpnHandler(
                            observedRequests,
                            { checkNotNull(scriptedResponses.removeFirstOrNull()) { "unexpected APNs request" } },
                            fault
                        ))
                    }
                })
                .bind(0)
                .syncUninterruptibly()
                .channel()
            return LocalApnsHttp2TlsServer(boss, workers, channel, certificate, observedRequests)
        }

        private fun serverContext(certificate: SelfSignedCertificate, supportedTlsProtocols: Array<String>): SslContext =
            SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey())
                .applicationProtocolConfig(h2AlpnConfig())
                .protocols(*supportedTlsProtocols)
                .ciphers(null, SupportedCipherSuiteFilter.INSTANCE)
                .build()
    }
}

enum class LocalApnsHttp2TlsFault { RESPOND, CLOSE_CONNECTION_AFTER_REQUEST }

data class CapturedApnsHttp2Request(
    val negotiatedApplicationProtocol: String,
    val negotiatedTlsProtocol: String,
    val method: String,
    val path: String,
    val authority: String,
    val headers: Map<String, String>,
    val body: String
)

private class ServerAlpnHandler(
    private val observedRequests: LinkedBlockingQueue<CapturedApnsHttp2Request>,
    private val responseForRequest: () -> APNsHttp2Response,
    private val fault: LocalApnsHttp2TlsFault
) : ApplicationProtocolNegotiationHandler("") {
    override fun configurePipeline(context: ChannelHandlerContext, protocol: String) {
        require(protocol == ApplicationProtocolNames.HTTP_2) { "expected ALPN h2, got $protocol" }
        val ssl = checkNotNull(context.pipeline().get(SslHandler::class.java))
        val tlsProtocol = ssl.engine().session.protocol
        context.pipeline().addLast(Http2FrameCodecBuilder.forServer().build())
        context.pipeline().addLast(
            Http2MultiplexHandler(object : ChannelInitializer<Http2StreamChannel>() {
                override fun initChannel(stream: Http2StreamChannel) {
                    stream.pipeline().addLast(ApnsRequestHandler(observedRequests, responseForRequest, fault, protocol, tlsProtocol))
                }
            })
        )
    }
}

private class ApnsRequestHandler(
    private val observedRequests: LinkedBlockingQueue<CapturedApnsHttp2Request>,
    private val responseForRequest: () -> APNsHttp2Response,
    private val fault: LocalApnsHttp2TlsFault,
    private val negotiatedProtocol: String,
    private val tlsProtocol: String
) : SimpleChannelInboundHandler<Http2StreamFrame>() {
    private var headers: Http2Headers? = null
    private val body = StringBuilder()

    override fun channelRead0(context: ChannelHandlerContext, frame: Http2StreamFrame) {
        when (frame) {
            is Http2HeadersFrame -> {
                headers = frame.headers()
                if (frame.isEndStream) completeAndRespond(context)
            }
            is Http2DataFrame -> {
                body.append(frame.content().toString(UTF_8))
                if (frame.isEndStream) completeAndRespond(context)
            }
        }
    }

    private fun completeAndRespond(context: ChannelHandlerContext) {
        val capturedHeaders = checkNotNull(headers)
        observedRequests.add(
            CapturedApnsHttp2Request(
                negotiatedApplicationProtocol = negotiatedProtocol,
                negotiatedTlsProtocol = tlsProtocol,
                method = capturedHeaders.method().toString(),
                path = capturedHeaders.path().toString(),
                authority = capturedHeaders.authority().toString(),
                headers = capturedHeaders
                    .filterNot { it.key.toString().startsWith(":") }
                    .associate { it.key.toString() to it.value.toString() },
                body = body.toString()
            )
        )

        if (fault == LocalApnsHttp2TlsFault.CLOSE_CONNECTION_AFTER_REQUEST) {
            context.channel().parent().close()
            return
        }
        val response = responseForRequest()
        val responseHeaders = DefaultHttp2Headers().status(response.statusCode.toString()).apply {
            response.apnsId?.let { set("apns-id", it) }
            response.headers.forEach { (name, value) -> set(name, value) }
        }
        val reasonBody = response.reason?.let { "{\"reason\":\"$it\"}" }
        if (reasonBody != null) {
            context.write(DefaultHttp2HeadersFrame(responseHeaders, false))
            context.writeAndFlush(DefaultHttp2DataFrame(Unpooled.copiedBuffer(reasonBody, UTF_8), true))
        } else {
            context.writeAndFlush(DefaultHttp2HeadersFrame(responseHeaders, true))
        }
    }
}

class LocalApnsHttp2TlsServerFixtureTest {
    @Test
    fun fixtureDecodesHttp2TlsAlpnRequestAndReturnsControlledApnsResponse() {
        LocalApnsHttp2TlsServer.start(
            APNsHttp2Response(statusCode = 200, apnsId = "apns-local", receivedAtEpochSeconds = 1_000)
        ).use { server ->
            val responseHeaders = sendHttp2Request(server)
            val request = server.awaitRequest()

            assertEquals(ApplicationProtocolNames.HTTP_2, request.negotiatedApplicationProtocol)
            assertTrue(request.negotiatedTlsProtocol == "TLSv1.2" || request.negotiatedTlsProtocol == "TLSv1.3")
            assertEquals("POST", request.method)
            assertEquals("/3/device/device-token", request.path)
            assertEquals("loopback.test:443", request.authority)
            assertEquals("com.guyghost.wakeve", request.headers.getValue("apns-topic"))
            assertEquals("{\"aps\":{}}", request.body)
            assertEquals("200", responseHeaders.getValue(":status"))
            assertEquals("apns-local", responseHeaders.getValue("apns-id"))
        }
    }

    private fun sendHttp2Request(server: LocalApnsHttp2TlsServer): Map<String, String> {
        val group = NioEventLoopGroup(1)
        val ready = CompletableFuture<Channel>()
        val responseHeaders = CompletableFuture<Map<String, String>>()
        try {
            val clientContext = SslContextBuilder.forClient()
                .trustManager(server.trustAnchor.cert())
                .applicationProtocolConfig(h2AlpnConfig())
                .build()
            Bootstrap().group(group).channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socket: SocketChannel) {
                        socket.pipeline().addLast(clientContext.newHandler(socket.alloc(), "localhost", server.endpoint.port))
                        socket.pipeline().addLast(object : ApplicationProtocolNegotiationHandler("") {
                            override fun configurePipeline(context: ChannelHandlerContext, protocol: String) {
                                require(protocol == ApplicationProtocolNames.HTTP_2) { "expected ALPN h2, got $protocol" }
                                context.pipeline().addLast(Http2FrameCodecBuilder.forClient().build())
                                context.pipeline().addLast(Http2MultiplexHandler(object : ChannelInitializer<Http2StreamChannel>() {
                                    override fun initChannel(stream: Http2StreamChannel) {
                                        stream.pipeline().addLast(Http2ResponseHeadersHandler(responseHeaders))
                                    }
                                }))
                                ready.complete(context.channel())
                            }
                        })
                    }
                })
                .connect("127.0.0.1", server.endpoint.port)
                .syncUninterruptibly()

            val parent = ready.get(5, TimeUnit.SECONDS)
            val stream = Http2StreamChannelBootstrap(parent)
                .handler(Http2ResponseHeadersHandler(responseHeaders))
                .open().syncUninterruptibly().getNow()
            val headers = DefaultHttp2Headers()
                .method("POST")
                .path("/3/device/device-token")
                .authority("loopback.test:443")
                .set("apns-topic", "com.guyghost.wakeve")
            stream.write(DefaultHttp2HeadersFrame(headers, false))
            stream.writeAndFlush(DefaultHttp2DataFrame(Unpooled.copiedBuffer("{\"aps\":{}}", UTF_8), true)).syncUninterruptibly()
            val observedResponse = responseHeaders.get(5, TimeUnit.SECONDS)
            parent.close().syncUninterruptibly()
            return observedResponse
        } finally {
            group.shutdownGracefully().syncUninterruptibly()
        }
    }
}

private class Http2ResponseHeadersHandler(
    private val responseHeaders: CompletableFuture<Map<String, String>>
) : SimpleChannelInboundHandler<Http2StreamFrame>() {
    override fun channelRead0(context: ChannelHandlerContext, frame: Http2StreamFrame) {
        if (frame is Http2HeadersFrame) {
            responseHeaders.complete(frame.headers().associate { it.key.toString() to it.value.toString() })
        }
    }
}

private fun h2AlpnConfig() = ApplicationProtocolConfig(
    ApplicationProtocolConfig.Protocol.ALPN,
    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
    ApplicationProtocolNames.HTTP_2
)
