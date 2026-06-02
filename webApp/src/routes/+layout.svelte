<script lang="ts">
  import { browser } from '$app/environment'
  import '../app.css'
  import { createAuthActor } from '$lib/actors/auth.actor.svelte'
  import type { Snippet } from 'svelte'

  interface Props { children: Snippet }
  const { children }: Props = $props()

  const auth = browser ? createAuthActor() : null

  $effect(() => {
    const actor = auth?.actor
    if (!actor) return

    const handler = () => actor.send({ type: 'LOGOUT' })
    window.addEventListener('wakeve:auth-expired', handler)
    return () => window.removeEventListener('wakeve:auth-expired', handler)
  })
</script>

{@render children()}
