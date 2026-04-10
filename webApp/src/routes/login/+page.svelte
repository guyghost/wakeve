<script lang="ts">
  import { goto } from '$app/navigation'
  import { useAuth } from '$lib/actors/auth.actor.svelte'
  import Spinner from '$lib/components/atoms/Spinner.svelte'

  const { snapshot, actor } = useAuth()

  // ── Reactive state ──────────────────────────────────────────────────────────
  let email = $state('')
  let otp = $state('')
  let emailError = $state('')

  const stateValue = $derived(snapshot.value as string)
  const ctxError = $derived(snapshot.context.error)
  const ctxEmail = $derived(snapshot.context.email)

  // Redirect when already authenticated
  $effect(() => {
    if (stateValue === 'authenticated') {
      goto('/')
    }
  })

  const isRequestingOtp = $derived(stateValue === 'requestingOtp')
  const isVerifyingOtp = $derived(stateValue === 'verifyingOtp')
  const isLoggingInGuest = $derived(stateValue === 'loggingInGuest')
  const isAnyLoading = $derived(isRequestingOtp || isVerifyingOtp || isLoggingInGuest)

  const showOtpStep = $derived(
    stateValue === 'enteringOtp' ||
    stateValue === 'verifyingOtp'
  )

  // ── Handlers ────────────────────────────────────────────────────────────────
  function validateEmail(value: string): boolean {
    const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
    if (!ok) emailError = 'Adresse email invalide'
    else emailError = ''
    return ok
  }

  function handleRequestOtp(e: SubmitEvent) {
    e.preventDefault()
    if (!validateEmail(email)) return
    actor.send({ type: 'REQUEST_OTP', email: email.trim() })
  }

  function handleVerifyOtp(e: SubmitEvent) {
    e.preventDefault()
    const trimmed = otp.replace(/\s/g, '')
    if (!trimmed) return
    actor.send({ type: 'VERIFY_OTP', otp: trimmed })
  }

  function handleGuest() {
    actor.send({ type: 'LOGIN_GUEST' })
  }

  function handleBack() {
    otp = ''
    actor.send({ type: 'BACK' })
  }

  function handleResend() {
    actor.send({ type: 'REQUEST_OTP', email: ctxEmail })
  }
</script>

<div class="min-h-screen bg-gradient-to-br from-wakeve-50 via-white to-blue-50 flex items-center justify-center p-4">
  <div class="w-full max-w-md">

    <!-- Logo above card -->
    <div class="flex flex-col items-center mb-6 gap-2">
      <div class="flex items-center gap-2">
        <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" class="h-10 w-10" aria-hidden="true">
          <rect width="32" height="32" rx="8" fill="#4f46e5" />
          <path d="M4 18c2-4 4-6 6-6s4 4 6 4 4-6 6-6 4 2 6 2" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span class="text-2xl font-bold text-wakeve-600">Wakeve</span>
      </div>
      <p class="text-sm text-gray-500 text-center">Planification collaborative d'événements</p>
    </div>

    <!-- Card -->
    <div class="bg-white rounded-2xl shadow-xl overflow-hidden">
      <!-- Gradient accent bar -->
      <div class="h-1.5 bg-gradient-to-r from-wakeve-600 to-wakeve-400"></div>

      <div class="px-8 py-8">

        <!-- Error banner (from machine context) -->
        {#if ctxError}
          <div
            role="alert"
            class="mb-5 flex items-start gap-2 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700"
          >
            <svg viewBox="0 0 20 20" fill="currentColor" class="h-5 w-5 shrink-0 mt-0.5" aria-hidden="true">
              <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495ZM10 5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 10 5Zm0 9a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" clip-rule="evenodd" />
            </svg>
            <span>{ctxError}</span>
          </div>
        {/if}

        <!-- ── Step 1: Email ─────────────────────────────────────────────── -->
        {#if !showOtpStep}
          <h1 class="text-xl font-bold text-gray-900 mb-1">Bienvenue sur Wakeve</h1>
          <p class="text-sm text-gray-500 mb-6">Connectez-vous pour planifier vos événements.</p>

          <form onsubmit={handleRequestOtp} class="flex flex-col gap-4" novalidate>
            <div class="flex flex-col gap-1">
              <label for="auth-email" class="text-sm font-medium text-gray-700">
                Adresse email
              </label>
              <input
                id="auth-email"
                type="email"
                bind:value={email}
                placeholder="votre@email.com"
                autocomplete="email"
                required
                disabled={isAnyLoading}
                aria-describedby={emailError ? 'email-error' : undefined}
                aria-invalid={!!emailError || undefined}
                class="w-full rounded-btn border px-3 py-2.5 text-sm text-gray-900 placeholder-gray-400 transition-default
                  {emailError ? 'border-red-400' : 'border-border'}
                  focus-visible:outline-wakeve-500 disabled:bg-gray-50"
              />
              {#if emailError}
                <p id="email-error" role="alert" class="text-xs text-red-600">{emailError}</p>
              {/if}
            </div>

            <button
              type="submit"
              disabled={isAnyLoading}
              class="w-full inline-flex items-center justify-center gap-2 rounded-btn bg-wakeve-600 px-4 py-2.5 text-sm font-semibold text-white
                hover:bg-wakeve-700 transition-default
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600
                disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {#if isRequestingOtp}
                <Spinner size="sm" color="currentColor" />
                Envoi en cours…
              {:else}
                Recevoir un code
              {/if}
            </button>
          </form>

          <div class="relative my-5">
            <div class="absolute inset-0 flex items-center" aria-hidden="true">
              <div class="w-full border-t border-gray-200"></div>
            </div>
            <div class="relative flex justify-center">
              <span class="bg-white px-3 text-xs text-gray-400">ou</span>
            </div>
          </div>

          <button
            type="button"
            disabled={isAnyLoading}
            onclick={handleGuest}
            class="w-full inline-flex items-center justify-center gap-2 rounded-btn border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-700
              hover:bg-gray-50 transition-default
              focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500
              disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {#if isLoggingInGuest}
              <Spinner size="sm" color="currentColor" />
              Connexion…
            {:else}
              Continuer en invité
            {/if}
          </button>

        <!-- ── Step 2: OTP ───────────────────────────────────────────────── -->
        {:else}
          <div class="flex items-center gap-3 mb-5">
            <button
              type="button"
              onclick={handleBack}
              class="rounded-btn p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-default
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500"
              aria-label="Retour"
            >
              <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4" aria-hidden="true">
                <path fill-rule="evenodd" d="M17 10a.75.75 0 0 1-.75.75H5.612l4.158 3.96a.75.75 0 1 1-1.04 1.08l-5.5-5.25a.75.75 0 0 1 0-1.08l5.5-5.25a.75.75 0 1 1 1.04 1.08L5.612 9.25H16.25A.75.75 0 0 1 17 10Z" clip-rule="evenodd" />
              </svg>
            </button>
            <div>
              <h1 class="text-xl font-bold text-gray-900">Vérifiez votre email</h1>
              <p class="text-sm text-gray-500">
                Code envoyé à <span class="font-medium text-gray-700">{ctxEmail}</span>
              </p>
            </div>
          </div>

          <!-- Email icon -->
          <div class="flex justify-center mb-6" aria-hidden="true">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-wakeve-50">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="h-8 w-8 text-wakeve-600">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" />
              </svg>
            </div>
          </div>

          <form onsubmit={handleVerifyOtp} class="flex flex-col gap-4">
            <div class="flex flex-col gap-1">
              <label for="auth-otp" class="text-sm font-medium text-gray-700">
                Code à 6 chiffres
              </label>
              <input
                id="auth-otp"
                type="text"
                inputmode="numeric"
                pattern="[0-9]{6}"
                maxlength={6}
                bind:value={otp}
                placeholder="123456"
                autocomplete="one-time-code"
                disabled={isVerifyingOtp}
                class="w-full rounded-btn border border-border px-3 py-2.5 text-center text-2xl tracking-[0.5em] font-mono text-gray-900 placeholder-gray-300 transition-default
                  focus-visible:outline-wakeve-500 disabled:bg-gray-50"
              />
            </div>

            <button
              type="submit"
              disabled={otp.replace(/\s/g, '').length < 6 || isVerifyingOtp}
              class="w-full inline-flex items-center justify-center gap-2 rounded-btn bg-wakeve-600 px-4 py-2.5 text-sm font-semibold text-white
                hover:bg-wakeve-700 transition-default
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-600
                disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {#if isVerifyingOtp}
                <Spinner size="sm" color="currentColor" />
                Vérification…
              {:else}
                Vérifier
              {/if}
            </button>
          </form>

          <p class="mt-4 text-center text-sm text-gray-500">
            Pas reçu le code ?
            <button
              type="button"
              onclick={handleResend}
              disabled={isAnyLoading}
              class="font-medium text-wakeve-600 hover:underline disabled:opacity-50
                focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wakeve-500 rounded"
            >
              Renvoyer
            </button>
          </p>
        {/if}

      </div>
    </div>

    <p class="text-center text-xs text-gray-400 mt-6">
      En continuant, vous acceptez nos conditions d'utilisation.
    </p>
  </div>
</div>
