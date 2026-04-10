<script lang="ts">
  import '../app.css'
  import { createAuthActor } from '$lib/actors/auth.actor.svelte'
  import type { Snippet } from 'svelte'

  interface Props { children: Snippet }
  const { children }: Props = $props()

  const { actor } = createAuthActor()

  $effect(() => {
    const handler = () => actor.send({ type: 'LOGOUT' })
    window.addEventListener('wakeve:auth-expired', handler)
    return () => window.removeEventListener('wakeve:auth-expired', handler)
  })
</script>

{@render children()}
