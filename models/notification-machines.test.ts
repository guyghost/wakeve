import assert from 'node:assert/strict'
import test from 'node:test'
import { backendUnregistrationTarget, classifyRegistrationBackendFailure, createIosNotificationRegistrationActor, iosNotificationRegistrationPortContract, permissionAllowsRemoteRegistration, registrationBackoff, registrationInvariants, validateRegistrationScope } from './ios-notification-registration.machine.ts'
import { classifyApnsResponse, deliveryIdentity, effectIdentity, fullJitterBackoffSeconds, nextProviderAuthSendCoordinator, nextProviderCircuit, recipientIdentity } from './notification-delivery.machine.ts'

const registration = (extra = {}) => {
  const actor = createIosNotificationRegistrationActor({ installationId: 'install-a', topic: 'com.guyghost.wakeve', environment: 'production', authSessionId: 'session-a', hasUsableJwt: true, ...extra })
  if (actor === null) throw new Error('valid registration scope must create an actor')
  return actor
}
test('launch checks permission and never prompts implicitly', () => { const a = registration(); assert.equal(a.getSnapshot().value, 'checkingPermission'); a.send({ type: 'APP_BECAME_ACTIVE' }); assert.equal(a.getSnapshot().value, 'checkingPermission') })
test('notDetermined requires explicit enable before permission request', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'notDetermined'); a.send({ type: 'PERMISSION_GRANTED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'notDetermined'); a.send({ type: 'USER_REQUESTED_ENABLE' }); assert.equal(a.getSnapshot().value, 'requestingPermission') })
test('allowed permission proceeds to APNs registration', () => { for (const status of ['authorized', 'provisional', 'ephemeral'] as const) { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status, correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'registeringApns') } })
test('denial exposes settings and never automatically prompts again', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'denied', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'USER_OPENED_SETTINGS' }); assert.equal(a.getSnapshot().value, 'denied'); a.send({ type: 'APP_BECAME_ACTIVE' }); assert.equal(a.getSnapshot().value, 'checkingPermission') })
test('registration cancellation is terminal before external association', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'USER_CANCELLED' }); assert.equal(a.getSnapshot().value, 'cancelled'); assert.equal(a.getSnapshot().status, 'done') })
test('APNs token waits for authentication and later resumes', () => { const a = registration({ authSessionId: null, hasUsableJwt: false }); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp-not-raw-token', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'awaitingAuthentication'); a.send({ type: 'AUTH_BECAME_AVAILABLE', authSessionId: 'session' }); assert.equal(a.getSnapshot().value, 'registeringBackend') })
test('backend transient failure is truthful and retryable', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_FAILED', failure: 'transient', correlationId: a.getSnapshot().context.correlationId!, nextRetryAt: 10 }); assert.equal(a.getSnapshot().value, 'retry'); assert.equal(a.getSnapshot().context.attempt, 1); a.send({ type: 'RETRY_DUE' }); assert.equal(a.getSnapshot().value, 'registeringBackend') })
test('missing raw APNs token while registering the backend is terminally misconfigured', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'registeringBackend'); a.send({ type: 'RAW_APNS_TOKEN_UNAVAILABLE', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'misconfigured'); assert.equal(a.getSnapshot().context.lastErrorClass, 'configuration'); assert.equal(a.getSnapshot().status, 'done') })
test('logout interrupts a retrying authenticated backend registration with installation unregister even before registration acknowledgement', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_FAILED', failure: 'transient', correlationId: a.getSnapshot().context.correlationId!, nextRetryAt: 10 }); assert.equal(a.getSnapshot().value, 'retry'); assert.equal(a.getSnapshot().context.backendRegistrationId, null); a.send({ type: 'LOGOUT_REQUESTED' }); assert.equal(a.getSnapshot().value, 'unregistering'); assert.equal(a.getSnapshot().context.hasUsableJwt, true); a.send({ type: 'BACKEND_UNREGISTER_SUCCEEDED', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'unregistered') })
test('configuration failure is terminal and fail closed', () => { const a = registration(); a.send({ type: 'CONFIGURATION_INVALID' }); assert.equal(a.getSnapshot().value, 'misconfigured'); assert.equal(a.getSnapshot().status, 'done') })
test('stale callback is ignored', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_SUCCEEDED', backendRegistrationId: 'r', correlationId: 'stale' }); assert.equal(a.getSnapshot().value, 'registeringBackend'); assert.equal(a.getSnapshot().context.backendRegistrationId, null) })
test('logout unregisters one installation before terminal credential clearance', () => { const a = registration({ backendRegistrationId: 'registration-a' }); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_SUCCEEDED', backendRegistrationId: 'registration-a', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'LOGOUT_REQUESTED' }); assert.equal(a.getSnapshot().value, 'unregistering'); assert.equal(a.getSnapshot().context.hasUsableJwt, true); a.send({ type: 'BACKEND_UNREGISTER_SUCCEEDED', correlationId: a.getSnapshot().context.correlationId! }); assert.equal(a.getSnapshot().value, 'unregistered') })
test('permission and retry helpers are deterministic and bounded', () => { assert.equal(permissionAllowsRemoteRegistration('provisional'), true); assert.equal(permissionAllowsRemoteRegistration('denied'), false); assert.equal(registrationBackoff(3, 0.5), 2_000); assert.equal(registrationBackoff(20, 1), 60_000) })
test('registration scope requires a topic and accepts only sandbox or production', () => { assert.equal(validateRegistrationScope('   ', 'production'), null); assert.deepEqual(validateRegistrationScope('com.guyghost.wakeve', 'sandbox'), { topic: 'com.guyghost.wakeve', environment: 'sandbox' }); assert.deepEqual(validateRegistrationScope('com.guyghost.wakeve', 'production'), { topic: 'com.guyghost.wakeve', environment: 'production' }); assert.equal(validateRegistrationScope('com.guyghost.wakeve', 'development'), null) })
test('registration actor factory rejects invalid scope before creating or starting an actor', () => { assert.equal(createIosNotificationRegistrationActor({ installationId: 'install-a', topic: '   ', environment: 'production' }), null); assert.equal(createIosNotificationRegistrationActor({ installationId: 'install-a', topic: 'com.guyghost.wakeve', environment: 'development' }), null) })
test('backend unregistration target is closed over known registration or idempotent installation fallback', () => { const known = registration({ backendRegistrationId: 'registration-a' }).getSnapshot().context; const unknown = registration().getSnapshot().context; assert.deepEqual(backendUnregistrationTarget(known), { kind: 'registration', registrationId: 'registration-a', installationId: 'install-a' }); assert.deepEqual(backendUnregistrationTarget(unknown), { kind: 'installation', installationId: 'install-a' }) })

test('registration backend HTTP failures use one closed status-only classifier for register and unregister', () => {
  const cases: Array<[number | null, boolean, 'authentication' | 'transient' | 'configuration']> = [
    [401, false, 'authentication'], [403, false, 'authentication'],
    [408, false, 'transient'], [429, false, 'transient'], [500, false, 'transient'], [503, false, 'transient'],
    [400, false, 'configuration'], [404, false, 'configuration'], [422, false, 'configuration'],
    [null, true, 'transient'], [400, true, 'transient'], [null, false, 'configuration'],
  ]
  for (const [statusCode, networkFailure, expected] of cases) {
    assert.equal(classifyRegistrationBackendFailure(statusCode, networkFailure), expected)
  }
  assert.equal(iosNotificationRegistrationPortContract.backendFailureClassifier, 'classifyRegistrationBackendFailure(statusCode, networkFailure)')
})

test('authenticated logout enters one unregistering flight from every reachable non-terminal registration state', () => {
  const cases: Array<[string, () => ReturnType<typeof registration>]> = [
    ['checkingPermission', () => registration()],
    ['notDetermined', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['requestingPermission', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'USER_REQUESTED_ENABLE' }); return a }],
    ['denied', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'denied', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['registeringApns', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['registeringBackend', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['retry', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_FAILED', failure: 'transient', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['registered', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_SUCCEEDED', backendRegistrationId: 'registration-a', correlationId: a.getSnapshot().context.correlationId! }); return a }],
  ]

  for (const [expectedStart, makeActor] of cases) {
    const actor = makeActor()
    assert.equal(actor.getSnapshot().value, expectedStart)
    actor.send({ type: 'LOGOUT_REQUESTED' })
    assert.equal(actor.getSnapshot().value, 'unregistering', expectedStart)
    assert.equal(actor.getSnapshot().context.logoutRequested, true, expectedStart)
    assert.equal(actor.getSnapshot().context.hasUsableJwt, true, expectedStart)
  }
})

test('logout without authentication is recoverable from every reachable non-terminal registration state', () => {
  const withoutAuth = () => ({ authSessionId: null, hasUsableJwt: false })
  const cases: Array<[string, () => ReturnType<typeof registration>]> = [
    ['checkingPermission', () => registration(withoutAuth())],
    ['notDetermined', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['requestingPermission', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'notDetermined', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'USER_REQUESTED_ENABLE' }); return a }],
    ['denied', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'denied', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['registeringApns', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['awaitingAuthentication', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['retry', () => { const a = registration(withoutAuth()); a.send({ type: 'PERMISSION_STATUS_FAILED', failure: 'transient', correlationId: a.getSnapshot().context.correlationId! }); return a }],
    ['registered', () => { const a = registration(); a.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'BACKEND_REGISTER_SUCCEEDED', backendRegistrationId: 'registration-a', correlationId: a.getSnapshot().context.correlationId! }); a.send({ type: 'AUTH_BECAME_UNAVAILABLE' }); return a }],
  ]

  for (const [expectedStart, makeActor] of cases) {
    const actor = makeActor()
    assert.equal(actor.getSnapshot().value, expectedStart)
    actor.send({ type: 'LOGOUT_REQUESTED' })
    assert.equal(actor.getSnapshot().value, 'retry', expectedStart)
    assert.equal(actor.getSnapshot().context.logoutRequested, true, expectedStart)
    assert.equal(actor.getSnapshot().context.resumeState, 'unregistering', expectedStart)
    assert.equal(actor.getSnapshot().context.lastErrorClass, 'authentication', expectedStart)
    assert.equal(actor.getSnapshot().context.hasUsableJwt, false, expectedStart)
    actor.send({ type: 'RETRY_DUE' })
    assert.equal(actor.getSnapshot().value, 'retry', `${expectedStart} must not emit a fictitious terminal`)
  }
})

test('logout without authentication stays recoverable and resumes installation fallback when auth returns', () => {
  const actor = registration({ authSessionId: null, hasUsableJwt: false })
  actor.send({ type: 'PERMISSION_STATUS_RESOLVED', status: 'authorized', correlationId: actor.getSnapshot().context.correlationId! })
  actor.send({ type: 'APNS_DID_REGISTER', tokenFingerprint: 'fp', correlationId: actor.getSnapshot().context.correlationId! })
  assert.equal(actor.getSnapshot().value, 'awaitingAuthentication')

  actor.send({ type: 'LOGOUT_REQUESTED' })
  assert.equal(actor.getSnapshot().value, 'retry')
  assert.equal(actor.getSnapshot().context.resumeState, 'unregistering')
  assert.equal(actor.getSnapshot().context.lastErrorClass, 'authentication')
  assert.equal(actor.getSnapshot().context.hasUsableJwt, false)
  assert.equal(actor.getSnapshot().status, 'active')
  actor.send({ type: 'RETRY_DUE' })
  assert.equal(actor.getSnapshot().value, 'retry')

  actor.send({ type: 'AUTH_BECAME_AVAILABLE', authSessionId: 'replacement-session' })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  assert.deepEqual(backendUnregistrationTarget(actor.getSnapshot().context), { kind: 'installation', installationId: 'install-a' })
})

test('authentication unregister failures are correlated bounded retries and retain credentials when blocked', () => {
  const actor = registration({ maxAttempts: 2 })
  actor.send({ type: 'LOGOUT_REQUESTED' })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  const firstCorrelation = actor.getSnapshot().context.correlationId!

  actor.send({ type: 'BACKEND_UNREGISTER_FAILED', failure: 'authentication', correlationId: 'stale' })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  assert.equal(actor.getSnapshot().context.attempt, 0)

  actor.send({ type: 'BACKEND_UNREGISTER_FAILED', failure: 'authentication', correlationId: firstCorrelation })
  assert.equal(actor.getSnapshot().value, 'retry')
  assert.equal(actor.getSnapshot().context.attempt, 1)
  assert.equal(actor.getSnapshot().context.resumeState, 'unregistering')
  assert.equal(actor.getSnapshot().context.hasUsableJwt, true)
  actor.send({ type: 'RETRY_DUE' })
  assert.equal(actor.getSnapshot().value, 'unregistering')

  actor.send({ type: 'BACKEND_UNREGISTER_FAILED', failure: 'authentication', correlationId: actor.getSnapshot().context.correlationId! })
  assert.equal(actor.getSnapshot().value, 'retry')
  assert.equal(actor.getSnapshot().context.attempt, 2)
  actor.send({ type: 'RETRY_DUE' })
  assert.equal(actor.getSnapshot().value, 'misconfigured')
  assert.equal(actor.getSnapshot().context.lastErrorClass, 'authentication')
  assert.equal(actor.getSnapshot().context.hasUsableJwt, true)
})

test('unregister retry renews correlation and only the second invocation success can terminate', () => {
  const actor = registration()
  actor.send({ type: 'LOGOUT_REQUESTED' })
  const firstCorrelation = actor.getSnapshot().context.correlationId!
  actor.send({ type: 'BACKEND_UNREGISTER_FAILED', failure: 'transient', correlationId: firstCorrelation })
  assert.equal(actor.getSnapshot().value, 'retry')

  actor.send({ type: 'RETRY_DUE' })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  const secondCorrelation = actor.getSnapshot().context.correlationId!
  assert.notEqual(secondCorrelation, firstCorrelation)

  actor.send({ type: 'BACKEND_UNREGISTER_SUCCEEDED', correlationId: firstCorrelation })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  assert.equal(actor.getSnapshot().context.correlationId, secondCorrelation)
  actor.send({ type: 'BACKEND_UNREGISTER_SUCCEEDED', correlationId: secondCorrelation })
  assert.equal(actor.getSnapshot().value, 'unregistered')
})

test('configuration failure blocks logout without emitting a fictitious success or clearing credentials', () => {
  const actor = registration()
  actor.send({ type: 'LOGOUT_REQUESTED' })
  actor.send({ type: 'BACKEND_UNREGISTER_FAILED', failure: 'configuration', correlationId: actor.getSnapshot().context.correlationId! })
  assert.equal(actor.getSnapshot().value, 'misconfigured')
  assert.equal(actor.getSnapshot().context.lastErrorClass, 'configuration')
  assert.equal(actor.getSnapshot().context.hasUsableJwt, true)
  assert.notEqual(actor.getSnapshot().value, iosNotificationRegistrationPortContract.logoutFlight.successTerminal)
})

test('logout flight has one transition authority and any number of read-only terminal observers', () => {
  assert.deepEqual(iosNotificationRegistrationPortContract.logoutFlight, {
    transitionAuthority: 'iosNotificationRegistration-actor-only',
    observerCardinality: 'zero-or-more-read-only-observers',
    observerTransitionAuthority: false,
    successTerminal: 'unregistered',
    successSignal: 'PUSH_UNREGISTERED',
    blockedTerminal: 'misconfigured',
    blockedResult: 'return-to-all-observers-without-clearing-credentials',
  })
  assert.ok(registrationInvariants.includes('only unregistered emits PUSH_UNREGISTERED; misconfigured returns a blocked result without clearing credentials'))

  const actor = registration()
  actor.send({ type: 'LOGOUT_REQUESTED' })
  const firstInvocation = actor.getSnapshot().context.invocationSequence
  const firstCorrelation = actor.getSnapshot().context.correlationId
  actor.send({ type: 'LOGOUT_REQUESTED' })
  assert.equal(actor.getSnapshot().value, 'unregistering')
  assert.equal(actor.getSnapshot().context.invocationSequence, firstInvocation)
  assert.equal(actor.getSnapshot().context.correlationId, firstCorrelation)
})

test('APNs classifier remains a closed status/reason matrix', () => {
  const reviewed = [
    [200, undefined, 'accepted'],
    [400, 'BadDeviceToken', 'invalidToken'], [410, 'Unregistered', 'invalidToken'],
    [400, 'IdleTimeout', 'retry'], [429, 'TooManyRequests', 'retry'], [500, undefined, 'retry'], [503, undefined, 'retry'],
    [400, 'BadTopic', 'rejectedPayload'], [404, undefined, 'rejectedPayload'], [413, undefined, 'rejectedPayload'],
    [403, 'ExpiredProviderToken', 'refreshAuth'], [403, 'InvalidProviderToken', 'providerAuthBlocked'],
    [418, 'NovelReason', 'unknownTerminal'],
  ] as const
  reviewed.forEach(([status, reason, expected]) => assert.equal(classifyApnsResponse(status, reason), expected))
})

test('provider circuit and per-send auth refresh remain deterministic', () => {
  const blocked = nextProviderCircuit({ blockedCredentialVersion: null }, { type: 'AUTH_BLOCKED', credentialVersion: 'der-a' })
  assert.equal(nextProviderCircuit(blocked, { type: 'VALIDATED_CREDENTIALS_CHANGED', credentialVersion: 'der-a' }).blockedCredentialVersion, 'der-a')
  assert.equal(nextProviderCircuit(blocked, { type: 'VALIDATED_CREDENTIALS_CHANGED', credentialVersion: 'der-b' }).blockedCredentialVersion, null)
  const first = nextProviderAuthSendCoordinator({ refreshUsed: false }, 'refreshAuth')
  assert.equal(first.action, 'refresh')
  assert.equal(nextProviderAuthSendCoordinator(first.coordinator, 'refreshAuth').action, 'block')
})

test('canonical layered identities are stable and injective', () => {
  const effect = effectIdentity('tenant:event:1', 'DATE_CONFIRMED', 2)
  const recipient = recipientIdentity(effect, 'participant-3', 'push')
  assert.equal(effect, effectIdentity('tenant:event:1', 'DATE_CONFIRMED', 2))
  assert.notEqual(effect, effectIdentity('tenant', 'event:1:DATE_CONFIRMED', 2))
  assert.equal(deliveryIdentity(recipient, 'registration-5', 'apns'), deliveryIdentity(recipient, 'registration-5', 'apns'))
  assert.notEqual(deliveryIdentity(recipient, 'registration-5', 'apns'), deliveryIdentity(recipient, 'registration-6', 'apns'))
})

test('delivery full jitter has a one-second floor and 300-second cap', () => {
  assert.equal(fullJitterBackoffSeconds(1, 0), 1)
  assert.equal(fullJitterBackoffSeconds(2, 0.5), 1)
  assert.equal(fullJitterBackoffSeconds(30, 1), 300)
})
