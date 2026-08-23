package com.guyghost.wakeve.notification

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http2.DefaultHttp2DataFrame
import io.netty.handler.codec.http2.DefaultHttp2Headers
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.handler.codec.http2.Http2StreamFrame
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.security.Signature
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

enum class APNsDeploymentEnvironment {
    DEVELOPMENT,
    PRODUCTION;

    companion object {
        fun parse(rawValue: String?): APNsDeploymentEnvironmentParse = when (rawValue?.trim()?.lowercase()) {
            "development" -> APNsDeploymentEnvironmentParse(DEVELOPMENT)
            "production" -> APNsDeploymentEnvironmentParse(PRODUCTION)
            null, "" -> APNsDeploymentEnvironmentParse(null, APNsProviderReadinessReason.MISSING_DEPLOYMENT_ENVIRONMENT)
            else -> APNsDeploymentEnvironmentParse(null, APNsProviderReadinessReason.INVALID_DEPLOYMENT_ENVIRONMENT)
        }
    }
}

data class APNsDeploymentEnvironmentParse(
    val environment: APNsDeploymentEnvironment?,
    val readinessReason: APNsProviderReadinessReason? = null
)

enum class APNsProviderReadinessStatus { READY, NOT_READY }

enum class APNsProviderReadinessReason {
    MISSING_KEY_ID, MISSING_TEAM_ID, MISSING_AUTH_KEY, INVALID_AUTH_KEY, MISSING_TOPIC,
    MISSING_ENVIRONMENT, MISSING_DEPLOYMENT_ENVIRONMENT, INVALID_DEPLOYMENT_ENVIRONMENT,
    PRODUCTION_REQUIRES_PRODUCTION_APNS
}

data class APNsProviderReadiness(
    val status: APNsProviderReadinessStatus,
    val reasons: Set<APNsProviderReadinessReason>
) {
    override fun toString() = "APNsProviderReadiness(status=$status, reasons=$reasons)"
}

/** Safe identity of the decoded P-256 PKCS#8 material; never the PEM representation or its hashCode. */
data class APNsSigningKeyFingerprint(val derSha256Hex: String) {
    companion object {
        fun fromPkcs8Pem(pem: String): APNsSigningKeyFingerprint {
            val der = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace(Regex("\\s"), "")
                .let { Base64.getDecoder().decode(it) }
            val hex = MessageDigest.getInstance("SHA-256").digest(der).joinToString("") { "%02x".format(it) }
            return APNsSigningKeyFingerprint(hex)
        }
    }
}

data class APNsProviderCredentialVersion(
    val keyId: String,
    val teamId: String,
    val signingKeyFingerprint: APNsSigningKeyFingerprint
) {
    companion object {
        fun from(config: APNsProviderConfig) = APNsProviderCredentialVersion(
            config.keyId,
            config.teamId,
            APNsSigningKeyFingerprint.fromPkcs8Pem(config.authKey)
        )
    }
}

/** TLS is restricted in production and can only be narrowed in the friend-scoped contract suite. */
class APNsTransportTlsConfiguration private constructor(val enabledProtocols: Set<String>) {
    init {
        require(enabledProtocols.isNotEmpty())
        require(enabledProtocols.all { it == "TLSv1.2" || it == "TLSv1.3" })
    }

    companion object {
        val apnsProduction = APNsTransportTlsConfiguration(linkedSetOf("TLSv1.2", "TLSv1.3"))

        internal fun forTests(enabledProtocols: Set<String>) = APNsTransportTlsConfiguration(enabledProtocols.toSet())
    }
}

/** ES256 token signer with one-hour tokens, rotating before the configured safety window. */
class ProductionAPNsTokenSigner(
    private val refreshSafetyWindowSeconds: Long = 300
) : APNsTokenSigner {
    private data class Cached(val credentialVersion: APNsProviderCredentialVersion, val token: APNsProviderToken)
    private var cached: Cached? = null
    private var onSignatureProduced: () -> Unit = {}
    private var onSignInvoked: () -> Unit = {}

    /** Friend-scoped observability for the APNs contract suite; production uses the primary constructor. */
    internal constructor(
        refreshSafetyWindowSeconds: Long = 300,
        onSignatureProduced: () -> Unit = {},
        onSignInvoked: () -> Unit = {}
    ) : this(refreshSafetyWindowSeconds) {
        this.onSignatureProduced = onSignatureProduced
        this.onSignInvoked = onSignInvoked
    }

    init { require(refreshSafetyWindowSeconds in 0..3_599) }

    override suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken> = synchronized(this) {
        runCatching {
        onSignInvoked()
        val now = clock.epochSeconds()
        val credentialVersion = APNsProviderCredentialVersion.from(config)
        cached?.takeIf { it.credentialVersion == credentialVersion && now < it.token.issuedAtEpochSeconds + 3_600 - refreshSafetyWindowSeconds }
            ?.let { return@synchronized Result.success(it.token) }
        val header = base64Url("{\"alg\":\"ES256\",\"kid\":\"${config.keyId}\"}")
        val payload = base64Url("{\"iss\":\"${config.teamId}\",\"iat\":$now}")
        val signed = "$header.$payload"
        val signer = Signature.getInstance("SHA256withECDSAinP1363Format")
        signer.initSign(APNsProviderConfig.parseP256Pkcs8(config.authKey))
        signer.update(signed.toByteArray(UTF_8))
        val token = APNsProviderToken("bearer $signed.${Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign())}", now)
        onSignatureProduced()
        cached = Cached(credentialVersion, token)
        token
        }
    }

    @Synchronized
    fun forceInvalidate() { cached = null }

    private fun base64Url(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(UTF_8))
}

data class APNsProviderRuntime(val signer: APNsTokenSigner, val transport: APNsHttp2Transport)

object APNsProviderRuntimeFactory {
    fun create(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderRuntime> = runCatching {
        APNsProviderRuntime(ProductionAPNsTokenSigner(), ProductionAPNsHttp2Transport(config, clock))
    }
}

/** One per-send coordinator: a correlated expired token permits exactly one forced refresh. */
enum class APNsProviderAuthRefreshAction { NONE, FORCE_REFRESH, BLOCK_PROVIDER }
class APNsProviderAuthRefreshCoordinator {
    private var refreshUsed = false
    fun actionFor(outcome: APNsProviderOutcome): APNsProviderAuthRefreshAction = when {
        outcome != APNsProviderOutcome.REFRESH_AUTH -> APNsProviderAuthRefreshAction.NONE
        !refreshUsed -> { refreshUsed = true; APNsProviderAuthRefreshAction.FORCE_REFRESH }
        else -> APNsProviderAuthRefreshAction.BLOCK_PROVIDER
    }
}

/**
 * Sender-scoped authentication circuit. Its state survives individual deliveries, but a
 * successfully validated replacement of the credentials is allowed to reset it.
 */
class APNsProviderCircuit {
    private var blockedCredentialVersion: APNsProviderCredentialVersion? = null

    @Synchronized
    fun isBlocked(credentialVersion: APNsProviderCredentialVersion): Boolean =
        blockedCredentialVersion == credentialVersion

    @Synchronized
    fun block(credentialVersion: APNsProviderCredentialVersion) {
        blockedCredentialVersion = credentialVersion
    }

    @Synchronized
    fun validatedCredentialsChanged(credentialVersion: APNsProviderCredentialVersion) {
        if (blockedCredentialVersion != credentialVersion) {
            blockedCredentialVersion = null
        }
    }
}

/** Real HTTP/2 + TLS + ALPN APNs transport. The loopback parameters are internal test-only seams. */
class ProductionAPNsHttp2Transport internal constructor(
    private val config: APNsProviderConfig,
    private val clock: APNsProviderClock,
    private val endpointOverride: InetSocketAddress? = null,
    private val trustAnchor: SelfSignedCertificate? = null,
    private val tlsConfiguration: APNsTransportTlsConfiguration = APNsTransportTlsConfiguration.apnsProduction
) : APNsHttp2Transport {
    override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
        val group = NioEventLoopGroup(1)
        var wrote = false
        try {
            val endpoint = endpointOverride?.let { InetSocketAddress("127.0.0.1", it.port) }
                ?: InetSocketAddress(config.environment.authority.substringBefore(':'), 443)
            val tlsPeerHost = if (endpointOverride == null) config.environment.authority.substringBefore(':') else "localhost"
            val ready = CompletableFuture<Channel>()
            val response = CompletableFuture<WireResponse>()
            val ssl = SslContextBuilder.forClient().apply {
                trustAnchor?.let { trustManager(it.cert()) }
                protocols(*tlsConfiguration.enabledProtocols.toTypedArray())
                applicationProtocolConfig(h2AlpnConfig())
            }.build()
            val parent = Bootstrap().group(group).channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socket: SocketChannel) {
                        socket.pipeline().addLast(ssl.newHandler(socket.alloc(), tlsPeerHost, endpoint.port))
                        socket.pipeline().addLast(object : ApplicationProtocolNegotiationHandler("") {
                            override fun configurePipeline(context: ChannelHandlerContext, protocol: String) {
                                require(protocol == ApplicationProtocolNames.HTTP_2) { "APNs requires ALPN h2" }
                                context.pipeline().addLast(Http2FrameCodecBuilder.forClient().build())
                                context.pipeline().addLast(Http2MultiplexHandler(object : ChannelInitializer<Http2StreamChannel>() {
                                    override fun initChannel(stream: Http2StreamChannel) {
                                        stream.pipeline().addLast(WireResponseHandler(response))
                                    }
                                }))
                                ready.complete(context.channel())
                            }
                        })
                    }
                }).connect(endpoint).syncUninterruptibly().channel()
            val h2 = ready.get(10, TimeUnit.SECONDS)
            val stream = Http2StreamChannelBootstrap(h2)
                .handler(WireResponseHandler(response))
                .open().syncUninterruptibly().getNow()
            val headers = DefaultHttp2Headers().method(request.method).path(request.path).authority(request.authority)
            request.headers.forEach { (name, value) -> headers.set(name, value) }
            wrote = true
            if (request.body.isEmpty()) {
                stream.writeAndFlush(DefaultHttp2HeadersFrame(headers, true)).syncUninterruptibly()
            } else {
                stream.write(DefaultHttp2HeadersFrame(headers, false))
                stream.writeAndFlush(DefaultHttp2DataFrame(Unpooled.copiedBuffer(request.body, UTF_8), true)).syncUninterruptibly()
            }
            val wire = response.get(15, TimeUnit.SECONDS)
            h2.close().syncUninterruptibly()
            return APNsTransportResult.Response(
                APNsHttp2Response(
                    statusCode = wire.status,
                    headers = wire.headers,
                    reason = wire.body.extractApnsReason(),
                    apnsId = wire.headers["apns-id"],
                    receivedAtEpochSeconds = clock.epochSeconds()
                )
            )
        } catch (_: Throwable) {
            val diagnostic = APNsSanitizedDiagnostic("transport", request.correlationId, errorClass = "transport")
            return if (wrote) APNsTransportResult.OutcomeUnknown(diagnostic) else APNsTransportResult.FailedBeforeWrite(diagnostic)
        } finally {
            group.shutdownGracefully().syncUninterruptibly()
        }
    }
}

private data class WireResponse(val status: Int, val headers: Map<String, String>, val body: String)
private class WireResponseHandler(private val completed: CompletableFuture<WireResponse>) : SimpleChannelInboundHandler<Http2StreamFrame>() {
    private var status = 0
    private var headers = emptyMap<String, String>()
    private val body = StringBuilder()
    override fun channelRead0(context: ChannelHandlerContext, frame: Http2StreamFrame) {
        when (frame) {
            is Http2HeadersFrame -> {
                status = frame.headers().status().toString().toInt()
                headers = frame.headers().filterNot { it.key.toString().startsWith(":") }.associate { it.key.toString() to it.value.toString() }
                if (frame.isEndStream) completed.complete(WireResponse(status, headers, body.toString()))
            }
            is Http2DataFrame -> {
                body.append(frame.content().toString(UTF_8))
                if (frame.isEndStream) completed.complete(WireResponse(status, headers, body.toString()))
            }
        }
    }
}

internal object APNsProviderRuntimeTestOverride {
    internal data class Value(val config: APNsProviderConfig, val deploymentEnvironment: APNsDeploymentEnvironment, val runtime: APNsProviderRuntime)
    private var value: Value? = null
    @Synchronized fun installLoopbackForTests(config: APNsProviderConfig, deploymentEnvironment: APNsDeploymentEnvironment, endpoint: InetSocketAddress, trustAnchor: SelfSignedCertificate, clock: APNsProviderClock): AutoCloseable {
        check(value == null) { "APNs loopback override is already installed" }
        value = Value(config, deploymentEnvironment, APNsProviderRuntime(ProductionAPNsTokenSigner(), ProductionAPNsHttp2Transport(config, clock, endpoint, trustAnchor)))
        return AutoCloseable { synchronized(this) { value = null } }
    }
    @Synchronized fun isInstalledForTests() = value != null
    @Synchronized internal fun current(): Value? = value
}

internal fun classifyApnsResponse(response: APNsHttp2Response): APNsProviderClassification {
    val reason = response.reason
    val outcome = when (response.statusCode) {
        200 -> APNsProviderOutcome.ACCEPTED
        400 -> when (reason) {
            "BadDeviceToken", "DeviceTokenNotForTopic" -> APNsProviderOutcome.INVALID_TOKEN
            "IdleTimeout" -> APNsProviderOutcome.RETRY
            "BadCollapseId", "BadMessageId", "BadTopic", "BadPath", "MethodNotAllowed",
            "PayloadEmpty", "PayloadTooLarge", "BadPriority", "BadExpirationDate", "MissingTopic" -> APNsProviderOutcome.REJECTED_PAYLOAD
            else -> APNsProviderOutcome.UNKNOWN_TERMINAL
        }
        410 -> when (reason) { "ExpiredToken", "Unregistered" -> APNsProviderOutcome.INVALID_TOKEN; else -> APNsProviderOutcome.UNKNOWN_TERMINAL }
        403 -> if (reason == "ExpiredProviderToken") APNsProviderOutcome.REFRESH_AUTH else APNsProviderOutcome.PROVIDER_AUTH_BLOCKED
        429 -> if (reason == "TooManyProviderTokenUpdates") APNsProviderOutcome.PROVIDER_AUTH_BLOCKED else APNsProviderOutcome.RETRY
        500, 503 -> APNsProviderOutcome.RETRY
        404, 405, 413 -> APNsProviderOutcome.REJECTED_PAYLOAD
        else -> APNsProviderOutcome.UNKNOWN_TERMINAL
    }
    val directive = if (outcome == APNsProviderOutcome.RETRY) APNsProviderRetryDirective(
        response.headers.entries.firstOrNull { it.key.equals("retry-after", true) }?.value?.toLongOrNull()?.let { response.receivedAtEpochSeconds + it }
    ) else null
    return APNsProviderClassification(outcome, response.statusCode, reason, response.apnsId, if (outcome == APNsProviderOutcome.ACCEPTED) response.receivedAtEpochSeconds else null, retryDirective = directive)
}

private fun String.extractApnsReason(): String? = Regex("\\\"reason\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(this)?.groupValues?.get(1)
private fun h2AlpnConfig() = ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN, ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE, ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2)
