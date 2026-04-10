<script lang="ts">
  import { goto } from '$app/navigation'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import AppHeader from '$lib/components/organisms/AppHeader.svelte'
  import AppNavMobile from '$lib/components/organisms/AppNavMobile.svelte'
  import Spinner from '$lib/components/atoms/Spinner.svelte'
  import type { Snippet } from 'svelte'

  interface Props { children: Snippet }
  const { children }: Props = $props()

  const { snapshot, actor } = useAuth()

  $effect(() => {
    const state = snapshot.value as string
    if (state === 'unauthenticated') {
      goto('/login')
    }
  })

  function handleLogout() {
    actor.send({ type: 'LOGOUT' })
    goto('/login')
  }
</script>

{#if snapshot.value === 'restoringSession' || snapshot.value === 'refreshing'}
  <div class="min-h-screen flex items-center justify-center bg-surface-alt">
    <div class="text-center">
      <Spinner size="lg" />
      <p class="mt-3 text-sm text-gray-500">Chargement…</p>
    </div>
  </div>

{:else if snapshot.value === 'authenticated'}
  <div class="min-h-screen bg-surface-alt">
    <AppHeader user={snapshot.context.user} onlogout={handleLogout} />
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-20 sm:pb-8">
      {@render children()}
    </main>
    <AppNavMobile />
  </div>
{/if}
