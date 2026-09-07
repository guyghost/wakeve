# Cycle QA Dark Mode — Uniformisation UX (DAO #27/#24/#25/#26)

Date : 2026-09-07
Périmètre : iOS, simulateur `Wakeve-QA-iPhone-16-Pro`, apparence système
**dark**, mêmes 8 routes QA headless que le cycle light
(`qa-screenshots/cycle-2026-09-07-dark/`).
Cible : residual risk de la revue design/a11y du 2026-09-07 (héro doré et
chip accent jamais mesurés en dark).

## Verdict global

**Approuvé après correction D1.** Le fond dark `midnightBlue #071421` est
mesuré exact sur 7/8 écrans (detail = canvas image plein écran par design) ;
les contrastes dark des textes et du CTA sont excellents ; un défaut introduit
par le correctif A2 light (hex fixe) est détecté et corrigé dans la foulée.

## Fonds mesurés (x=8, y=1200) — attendu midnight #071421 (7,20,33)

| Écran | Mesure | Verdict |
|---|---|---|
| library | (7,20,33) | ✅ |
| studio | (7,20,33) | ✅ |
| poll | (7,20,33) | ✅ |
| poll-results | (7,20,33) | ✅ |
| organization | (7,19,32) — arrondi rendu | ✅ |
| information | (7,20,33) | ✅ |
| archive | (7,20,33) | ✅ |
| detail | canvas invitation plein écran | par design |

## Contrastes dark (WCAG 2.1)

### Passés

| Pair | Ratio | Seuil |
|---|---|---|
| Texte primaire blanc sur midnight | 18.57 | 4.5 ✅ |
| Texte secondaire (blanc 66%) sur midnight | 8.42 | 4.5 ✅ |
| Texte tertiaire (blanc 48%) sur midnight | 4.97 | 4.5 ✅ |
| CTA dark : texte `appDark` sur pilule blanche 92% | 15.06 | 4.5 ✅ |

### D1 — Chip de statut Scénarios en dark : 2.09 — **corrigé**

Le correctif A2 light (foreground `#1D4ED8` fixe) dégradait le dark :
`#1D4ED8` sur fond `paleBlue 14%`/midnight = **2.09** (< 4.5). Le fond chip
suit l'accent natif (paleBlue en dark), le texte fixe light n'avait plus de
sens.

**Correctif appliqué** : foreground cross-scheme —
`colorScheme == .dark ? SemanticColor.accent : #1D4ED8` :
- light : `#1D4ED8` sur accent 14%/ivoire → **4.94** ✅ (inchangé)
- dark : paleBlue sur accent 14%/midnight → **8.03** ✅

### A1-dark — Bordure dorée du héro : 1.94 vs 3.0 (non-texte) — qualifié

`subtleAmber` à 30% sur midnight = 1.94 (mieux qu'en light 1.36, toujours
sous le seuil non-texte). Statut inchangé : bordure **décorative**, le signal
« confirmé » est porté par l'étoile amber pleine + le libellé explicite —
exigence déjà documentée sur `WakeveHeroCard`.

## Fichiers modifiés

- `iosApp/src/Views/Events/ScenarioOrganizationView.swift` : foreground
  phaseBadge cross-scheme (D1)
- `qa-screenshots/cycle-2026-09-07-dark/` : 8 captures dark

## Résidual risk

- L'émulateur ne reproduit pas la vraie luminosité OLED : mesure device
  physique recommandée avant release.
- Les états translucides (glassCard materials) sous « Reduce Transparency »
  en dark restent à capturer (l'app expose `--wakeve-qa-reduce-transparency`
  pour ce cycle dédié).
