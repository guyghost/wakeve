<svelte:head>
  <title>Wakeve app for iPhone</title>
  <meta
    name="description"
    content="Wakeve transforme les discussions de groupe en événements clairs: idée, planification, organisation et célébration."
  />
</svelte:head>

<script lang="ts">
  import { onMount } from 'svelte'

  const navItems = [
    { label: 'Product', href: '#demo' },
    { label: 'Company', href: '#time' },
    { label: 'Resources', href: '#footer' }
  ]

  const floatingScreens = [
    {
      title: 'Création',
      subtitle: 'Dîner rooftop',
      accent: 'screen-blue',
      items: ['Titre', 'Invités', 'Créneaux']
    },
    {
      title: 'Sondage',
      subtitle: '21 juin gagne',
      accent: 'screen-rose',
      items: ['Oui 8', 'Maybe 3', 'Non 1']
    },
    {
      title: 'Preview',
      subtitle: 'Tout est prêt',
      accent: 'screen-violet',
      items: ['Lieu validé', 'Budget clair', 'Rappels']
    }
  ]

  const messyNotes = [
    'Qui peut venir ?',
    'On fait samedi ?',
    'Budget max ?',
    'Train ou voiture ?',
    'Je ramène quoi ?'
  ]

  onMount(() => {
    const animatedElements = document.querySelectorAll<HTMLElement>('[data-animate]')
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer.unobserve(entry.target)
          }
        }
      },
      { rootMargin: '0px 0px -12% 0px', threshold: 0.18 }
    )

    animatedElements.forEach((element) => observer.observe(element))
    return () => observer.disconnect()
  })
</script>

<main class="landing-page">
  <header class="site-header" data-animate>
    <a href="/" class="brand-mark" aria-label="Wakeve">
      <svg viewBox="0 0 32 32" fill="none" class="brand-icon" aria-hidden="true">
        <rect width="32" height="32" rx="8" fill="#0f172a" />
        <path
          d="M5 18.2c2.15-4.3 4.1-6.2 6.05-6.2 2.1 0 3.65 4 5.95 4 2.15 0 3.55-5.75 5.95-5.75 1.95 0 3.35 1.55 4.05 2.2"
          stroke="white"
          stroke-width="2.35"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
      <span>Wakeve</span>
    </a>

    <nav class="site-nav" aria-label="Navigation principale">
      {#each navItems as item}
        <a href={item.href}>{item.label}</a>
      {/each}
    </nav>

    <a class="download-link" href="/app/login">Download the app</a>
  </header>

  <section class="hero-section">
    <div class="hero-copy" data-animate>
      <h1>Wakeve app for iPhone</h1>
      <p>Build and launch mobile apps &amp; websites from your phone.</p>
      <div class="hero-actions">
        <a class="button button-dark" href="/app/login">Telecharger l'application</a>
        <a class="button button-quiet" href="#demo">En savoir plus</a>
      </div>
    </div>

    <div class="hero-visual" aria-label="Aperçu animé de Wakeve sur iPhone" data-animate>
      <div class="soft-panel"></div>
      <div class="phone-hand" aria-hidden="true">
        <span class="finger finger-one"></span>
        <span class="finger finger-two"></span>
        <span class="finger finger-three"></span>
        <span class="thumb"></span>
      </div>

      <div class="iphone hero-phone">
        <div class="dynamic-island"></div>
        <div class="screen-stack">
          <section class="app-screen screen-events">
            <div class="screen-header">
              <span>Wakeve</span>
              <strong>9:41</strong>
            </div>
            <div class="event-card-primary">
              <p>Ce soir</p>
              <h2>Anniversaire de Lina</h2>
              <span>12 amis · Paris 11</span>
            </div>
            <div class="color-list">
              <span style="--swatch:#A7F3D0">Dîner</span>
              <span style="--swatch:#FBCFE8">Sondage</span>
              <span style="--swatch:#C7D2FE">Budget</span>
            </div>
          </section>

          <section class="app-screen screen-poll">
            <div class="screen-header">
              <span>Sondage</span>
              <strong>21 juin</strong>
            </div>
            <div class="poll-card winning">
              <span>Samedi</span>
              <strong>8 oui</strong>
            </div>
            <div class="poll-card">
              <span>Vendredi</span>
              <strong>5 maybe</strong>
            </div>
            <div class="poll-card muted">
              <span>Dimanche</span>
              <strong>2 oui</strong>
            </div>
          </section>

          <section class="app-screen screen-plan">
            <div class="screen-header">
              <span>Plan final</span>
              <strong>91%</strong>
            </div>
            <div class="plan-map"></div>
            <div class="plan-row"><span>Transport</span><strong>Réservé</strong></div>
            <div class="plan-row"><span>Budget</span><strong>184 EUR</strong></div>
            <div class="plan-row"><span>Rappels</span><strong>Actifs</strong></div>
          </section>
        </div>
      </div>

      <div class="hero-chip chip-one">Build real plans, not chaos</div>
      <div class="hero-chip chip-two">Use your voice to organize</div>
      <div class="hero-chip chip-three">Celebrate what you planned</div>
    </div>
  </section>

  <section id="demo" class="demo-section" data-animate>
    <div class="section-heading">
      <h2>Une démo qui respire avec votre organisation.</h2>
      <p>Chaque écran suit le groupe: création d'événement, sondage, preview et plan final.</p>
    </div>

    <div class="floating-phone-stage">
      {#each floatingScreens as screen, index}
        <article class={`mini-phone mini-phone-${index + 1}`} style={`--delay:${index * 110}ms`}>
          <div class="mini-island"></div>
          <div class={`mini-preview ${screen.accent}`}></div>
          <h3>{screen.title}</h3>
          <p>{screen.subtitle}</p>
          <div class="mini-list">
            {#each screen.items as item}
              <span>{item}</span>
            {/each}
          </div>
        </article>
      {/each}
    </div>
  </section>

  <section id="time" class="time-section" data-animate>
    <div class="time-copy">
      <h2>Gagnez du temps.</h2>
      <p>
        Wakeve récupère les conversations en désordre et les transforme en étapes simples:
        décider, confirmer, organiser, puis profiter.
      </p>
    </div>

    <div class="conversion-visual">
      <div class="messy-notes" aria-label="Conversations en désordre">
        {#each messyNotes as note, index}
          <span class={`note note-${index + 1}`}>{note}</span>
        {/each}
      </div>
      <div class="iphone conversion-phone">
        <div class="dynamic-island"></div>
        <div class="clean-plan">
          <p>Wakeve a rangé le plan</p>
          <h3>Samedi · 20:30</h3>
          <div class="clean-step done">Date confirmée</div>
          <div class="clean-step done">Invités prévenus</div>
          <div class="clean-step active">Budget partagé</div>
        </div>
      </div>
    </div>
  </section>

  <section class="final-section" data-animate>
    <h2>Idée, Planification, Organisation et Célébration. Tout en un.</h2>
    <p>La meilleure manière de se retrouver.</p>
    <a class="button button-dark" href="/app/login">Telecharger l'application</a>
  </section>

  <footer id="footer" class="site-footer" data-animate>
    <div class="footer-brand">
      <a href="/" class="brand-mark" aria-label="Wakeve">
        <svg viewBox="0 0 32 32" fill="none" class="brand-icon" aria-hidden="true">
          <rect width="32" height="32" rx="8" fill="#0f172a" />
          <path
            d="M5 18.2c2.15-4.3 4.1-6.2 6.05-6.2 2.1 0 3.65 4 5.95 4 2.15 0 3.55-5.75 5.95-5.75 1.95 0 3.35 1.55 4.05 2.2"
            stroke="white"
            stroke-width="2.35"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span>Wakeve</span>
      </a>
      <p>Wakeve est disponible</p>
      <a class="button button-dark footer-button" href="/app/login">Download the app</a>
    </div>

    <div class="footer-links">
      <div>
        <h3>Product</h3>
        <a href="#demo">Demo</a>
        <a href="#time">Planning</a>
      </div>
      <div>
        <h3>Company</h3>
        <a href="/support">Support</a>
        <a href="/privacy">Privacy</a>
      </div>
      <div>
        <h3>Resources</h3>
        <a href="/terms">Terms</a>
        <a href="/third-party-notices">Notices</a>
      </div>
    </div>
  </footer>

  <div class="made-in-framer" aria-label="Made in Framer">Made in Framer</div>
</main>
