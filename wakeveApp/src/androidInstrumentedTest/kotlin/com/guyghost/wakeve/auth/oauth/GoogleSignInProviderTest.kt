package com.guyghost.wakeve.auth.oauth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthToken
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.shell.services.GoogleSignInProvider
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for Google Sign-In provider.
 *
 * Tests the complete Google Sign-In integration flow:
 * - GoogleSignInClient creation with correct configuration
 * - Sign-in intent retrieval
 * - Sign-in result handling (success, cancelled, failed)
 * - User data extraction and AuthResult creation
 * - Error handling for various scenarios
 *
 * Uses MockK for mocking Google Play Services components.
 */
@RunWith(AndroidJUnit4::class)
class GoogleSignInProviderTest {

    private lateinit var provider: GoogleSignInProvider
    private lateinit var context: Context
    private val webClientId = "test-web-client-id"

    // Mocks
    private lateinit var mockGoogleSignIn: GoogleSignIn
    private lateinit var mockGoogleSignInClient: GoogleSignInClient
    private lateinit var mockGoogleSignInOptionsBuilder: GoogleSignInOptions.Builder
    private lateinit var mockGoogleSignInOptions: GoogleSignInOptions
    private lateinit var mockTask: Task<GoogleSignInAccount>
    private lateinit var mockGoogleSignInAccount: GoogleSignInAccount

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        provider = GoogleSignInProvider()

        // Mock static methods and classes
        mockkStatic(GoogleSignIn::class)
        mockkConstructor(GoogleSignInOptions.Builder::class)
        
        // Setup common mocks
        mockGoogleSignInClient = mockk()
        mockGoogleSignInOptions = mockk()
        mockGoogleSignInOptionsBuilder = mockk()
        mockTask = mockk()
        mockGoogleSignInAccount = mockk()

        // Setup default mock behaviors
        every { GoogleSignIn.getClient(any(), any()) } returns mockGoogleSignInClient
        every { GoogleSignInOptions.Builder(any()) } returns mockGoogleSignInOptionsBuilder
        every { mockGoogleSignInOptionsBuilder.requestIdToken(any()) } returns mockGoogleSignInOptionsBuilder
        every { mockGoogleSignInOptionsBuilder.requestEmail() } returns mockGoogleSignInOptionsBuilder
        every { mockGoogleSignInOptionsBuilder.requestProfile() } returns mockGoogleSignInOptionsBuilder
        every { mockGoogleSignInOptionsBuilder.build() } returns mockGoogleSignInOptions
    }

    // ==================== createGoogleSignInClient Tests ====================

    /**
     * Test createGoogleSignInClient creates client with correct options.
     *
     * GIVEN a context and web client ID
     * WHEN calling createGoogleSignInClient
     * THEN GoogleSignIn.getClient is called with correct options
     */
    @Test
    fun `createGoogleSignInClient configures options correctly`() = runTest {
        // ACT
        val result = provider.createGoogleSignInClient(context, webClientId)

        // ASSERT
        assertNotNull(result)
        assertEquals(mockGoogleSignInClient, result)
        
        verify { 
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            mockGoogleSignInOptionsBuilder.requestIdToken(webClientId)
            mockGoogleSignInOptionsBuilder.requestEmail()
            mockGoogleSignInOptionsBuilder.requestProfile()
            mockGoogleSignInOptionsBuilder.build()
            GoogleSignIn.getClient(context, mockGoogleSignInOptions)
        }
    }

    /**
     * Test createGoogleSignInClient returns configured client.
     *
     * GIVEN mocked GoogleSignInClient
     * WHEN calling createGoogleSignInClient
     * THEN the mocked client is returned
     */
    @Test
    fun `createGoogleSignInClient returns mocked client`() = runTest {
        // ACT
        val result = provider.createGoogleSignInClient(context, webClientId)

        // ASSERT
        assertEquals(mockGoogleSignInClient, result)
    }

    // ==================== getGoogleSignInIntent Tests ====================

    /**
     * Test getGoogleSignInIntent returns sign-in intent.
     *
     * GIVEN a GoogleSignInClient
     * WHEN calling getGoogleSignInIntent
     * THEN the client's signInIntent is returned
     */
    @Test
    fun `getGoogleSignInIntent returns client intent`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        every { mockGoogleSignInClient.signInIntent } returns mockIntent

        // ACT
        val result = provider.getGoogleSignInIntent(mockGoogleSignInClient)

        // ASSERT
        assertEquals(mockIntent, result)
        verify { mockGoogleSignInClient.signInIntent }
    }

    // ==================== handleGoogleSignInResult Success Tests ====================

    /**
     * Test handleGoogleSignInResult success with complete user data.
     *
     * GIVEN a successful Google sign-in result
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Success is returned with user data and token
     */
    @Test
    fun `handleGoogleSignInResult success creates AuthResult with complete user data`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val testName = "Test User"
        val testIdToken = "test-id-token"
        
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns testEmail
        every { mockGoogleSignInAccount.displayName } returns testName
        every { mockGoogleSignInAccount.idToken } returns testIdToken

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        
        assertEquals(testId, successResult.user.id)
        assertEquals(testEmail, successResult.user.email)
        assertEquals(testName, successResult.user.name)
        assertEquals(AuthMethod.GOOGLE, successResult.user.authMethod)
        assertEquals(testIdToken, successResult.token.value)
        assertEquals(com.guyghost.wakeve.auth.core.models.TokenType.BEARER, successResult.token.type)
    }

    /**
     * Test handleGoogleSignInResult success with minimal user data (no name).
     *
     * GIVEN a successful Google sign-in result with no display name
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Success is returned with name as null
     */
    @Test
    fun `handleGoogleSignInResult success handles null display name`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val testIdToken = "test-id-token"
        
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns testEmail
        every { mockGoogleSignInAccount.displayName } returns null
        every { mockGoogleSignInAccount.idToken } returns testIdToken

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        
        assertEquals(testId, successResult.user.id)
        assertEquals(testEmail, successResult.user.email)
        assertEquals(null, successResult.user.name)
        assertEquals(AuthMethod.GOOGLE, successResult.user.authMethod)
    }

    // ==================== handleGoogleSignInResult Error Tests ====================

    /**
     * Test handleGoogleSignInResult handles user cancellation.
     *
     * GIVEN a cancelled Google sign-in result (status code 12501)
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error with OAuthCancelled is returned
     */
    @Test
    fun `handleGoogleSignInResult cancelled returns OAuthCancelled error`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        val apiException = mockk<ApiException>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } throws apiException
        every { apiException.statusCode } returns CommonStatusCodes.CANCELED

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        
        assertTrue(errorResult.error is AuthError.OAuthCancelled)
        val oauthError = errorResult.error as AuthError.OAuthCancelled
        assertEquals(AuthMethod.GOOGLE, oauthError.provider)
    }

    /**
     * Test handleGoogleSignInResult handles sign-in required (status code 4).
     *
     * GIVEN a sign-in required Google sign-in result
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error with appropriate message is returned
     */
    @Test
    fun `handleGoogleSignInResult sign in required returns OAuthError`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        val apiException = mockk<ApiException>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } throws apiException
        every { apiException.statusCode } returns 4

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        
        assertTrue(errorResult.error is AuthError.OAuthError)
        val oauthError = errorResult.error as AuthError.OAuthError
        assertEquals(AuthMethod.GOOGLE, oauthError.provider)
        assertEquals("Ré-authentification requise", oauthError.message)
    }

    /**
     * Test handleGoogleSignInResult handles generic sign-in failure.
     *
     * GIVEN a failed Google sign-in result with unknown status code
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error with status code in message is returned
     */
    @Test
    fun `handleGoogleSignInResult failed returns OAuthError with status code`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        val apiException = mockk<ApiException>()
        val testStatusCode = 10
        val testMessage = "Test error message"
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } throws apiException
        every { apiException.statusCode } returns testStatusCode
        every { apiException.message } returns testMessage

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        
        assertTrue(errorResult.error is AuthError.OAuthError)
        val oauthError = errorResult.error as AuthError.OAuthError
        assertEquals(AuthMethod.GOOGLE, oauthError.provider)
        assertEquals("Erreur Google Sign-In: $testStatusCode - $testMessage", oauthError.message)
    }

    // ==================== handleGoogleSignInResult Null Account Tests ====================

    /**
     * Test handleGoogleSignInResult handles null account from task result.
     *
     * GIVEN a task that returns null account
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult null account returns error`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns null

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    /**
     * Test handleGoogleSignInResult handles null account ID.
     *
     * GIVEN an account with null ID
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult null account ID returns error`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns null

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    /**
     * Test handleGoogleSignInResult handles null email.
     *
     * GIVEN an account with null email
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult null email returns error`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns null

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    /**
     * Test handleGoogleSignInResult handles null ID token.
     *
     * GIVEN an account with null ID token
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult null ID token returns error`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns testEmail
        every { mockGoogleSignInAccount.idToken } returns null

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    /**
     * Test handleGoogleSignInResult handles empty ID token.
     *
     * GIVEN an account with empty ID token
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult empty ID token returns error`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns testEmail
        every { mockGoogleSignInAccount.idToken } returns ""

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    // ==================== handleGoogleSignInResult Unexpected Error Tests ====================

    /**
     * Test handleGoogleSignInResult handles unexpected exceptions.
     *
     * GIVEN an unexpected exception during sign-in processing
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error with generic error message is returned
     */
    @Test
    fun `handleGoogleSignInResult unexpected exception returns OAuthError`() = runTest {
        // ARRANGE
        val mockIntent = mockk<android.content.Intent>()
        val unexpectedException = RuntimeException("Unexpected error")
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } throws unexpectedException

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        
        assertTrue(errorResult.error is AuthError.OAuthError)
        val oauthError = errorResult.error as AuthError.OAuthError
        assertEquals(AuthMethod.GOOGLE, oauthError.provider)
        assertEquals("Erreur inattendue: Unexpected error", oauthError.message)
    }

    // ==================== signOut Tests ====================

    /**
     * Test signOut calls client signOut.
     *
     * GIVEN a GoogleSignInClient
     * WHEN calling signOut
     * THEN the client's signOut method is called
     */
    @Test
    fun `signOut calls client signOut`() = runTest {
        // ARRANGE
        every { mockGoogleSignInClient.signOut() } returns mockk()

        // ACT
        provider.signOut(mockGoogleSignInClient)

        // ASSERT
        verify { mockGoogleSignInClient.signOut() }
    }

    // ==================== handleGoogleSignInResult Null Intent Tests ====================

    /**
     * Test handleGoogleSignInResult handles null intent.
     *
     * GIVEN a null intent
     * WHEN calling handleGoogleSignInResult
     * THEN AuthResult.Error is returned
     */
    @Test
    fun `handleGoogleSignInResult null intent returns error`() = runTest {
        // ACT
        val result = provider.handleGoogleSignInResult(null)

        // ASSERT
        assertTrue(result is AuthResult.Error)
        val errorResult = result as AuthResult.Error
        assertTrue(errorResult.error is AuthError.OAuthError)
    }

    // ==================== Token Validation Tests ====================

    /**
     * Test created AuthToken has correct properties.
     *
     * GIVEN a successful Google sign-in
     * WHEN creating AuthResult
     * THEN AuthToken is created with correct type and expiration
     */
    @Test
    fun `handleGoogleSignInResult creates AuthToken with correct properties`() = runTest {
        // ARRANGE
        val testId = "google-user-123"
        val testEmail = "test@gmail.com"
        val testName = "Test User"
        val testIdToken = "test-id-token"
        
        val mockIntent = mockk<android.content.Intent>()
        
        every { GoogleSignIn.getSignedInAccountFromIntent(mockIntent) } returns mockTask
        every { mockTask.getResult(ApiException::class) } returns mockGoogleSignInAccount
        every { mockGoogleSignInAccount.id } returns testId
        every { mockGoogleSignInAccount.email } returns testEmail
        every { mockGoogleSignInAccount.displayName } returns testName
        every { mockGoogleSignInAccount.idToken } returns testIdToken

        // ACT
        val result = provider.handleGoogleSignInResult(mockIntent)

        // ASSERT
        assertTrue(result is AuthResult.Success)
        val successResult = result as AuthResult.Success
        
        assertEquals(testIdToken, successResult.token.value)
        assertEquals(com.guyghost.wakeve.auth.core.models.TokenType.BEARER, successResult.token.type)
        
        // Verify expiration is set to 30 days from now (allowing small time difference)
        val expectedExpiration = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        val actualExpiration = successResult.token.expiresAt
        val timeDifference = kotlin.math.abs(expectedExpiration - actualExpiration)
        assertTrue(timeDifference < 5000) // Allow 5 seconds difference
    }
}