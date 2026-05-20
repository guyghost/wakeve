import { createActor } from 'xstate'
import { authMachine } from '$lib/machines/auth.machine'
import { getContext, setContext } from 'svelte'

const AUTH_CONTEXT_KEY = Symbol('wakeve-auth')

// Derive the actor type from the machine so it stays in sync automatically
type AuthActor = ReturnType<typeof createActor<typeof authMachine>>
type AuthSnapshot = ReturnType<AuthActor['getSnapshot']>

export interface AuthActorContext {
  readonly snapshot: AuthSnapshot
  readonly actor: AuthActor
}

/**
 * Instantiate, start and register the auth actor in Svelte context.
 * Must be called **once** inside the root layout component
 * (i.e. during component initialisation, not in a callback).
 *
 * @returns The reactive context object — use `ctx.snapshot` in templates.
 */
export function createAuthActor(): AuthActorContext {
  const actor = createActor(authMachine)
  actor.start()

  // Svelte 5 reactive state — updated on every machine transition
  let snapshot = $state<AuthSnapshot>(actor.getSnapshot())
  actor.subscribe((s) => {
    snapshot = s
  })

  const ctx: AuthActorContext = {
    get snapshot() {
      return snapshot
    },
    actor
  }

  setContext(AUTH_CONTEXT_KEY, ctx)
  return ctx
}

/**
 * Retrieve the auth actor context registered by `createAuthActor()`.
 * Call this in any child component that needs auth state.
 */
export function useAuth(): AuthActorContext {
  return getContext<AuthActorContext>(AUTH_CONTEXT_KEY)
}
