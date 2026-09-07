# Revue Design & Accessibilité — Uniformisation UX (DAO #27/#24/#25/#26)

Date : 2026-09-07
Revue : @review (design system + accessibilité) — feedback uniquement
Périmètre : captures `qa-screenshots/cycle-2026-09-07/` (8 écrans, iPhone 16 Pro,
light mode), code des composants livrés, arbitrages demandés.
Référence design : écran Résultats `qa-screenshots/cycle-2026-09-04/28-poll-results.png`.

## Verdict global

**Approuvé avec réserves** — l'uniformisation est fidèle à l'écran de référence :
fond ivoire `#F6F1EA` mesuré exact sur 6 écrans de contenu, en-têtes unifiés,
carte héro dorée réservée à l'info confirmée, un seul bleu d'action `#2563EB`.
Deux findings WCAG AA à traiter (dont un sans changement de code requis) et
une correction d'arbitrage studio recommandée avant le prochain ship.

## Conformité design system (modèle écran Résultats)

| Axe | Constat | Verdict |
|---|---|---|
| Fond de page | Ivoire `#F6F1EA` mesuré au pixel sur library, poll, poll-results, organization, information, archive | ✅ Conforme |
| En-têtes | Grand titre display + sous-titre contexte (WakeveScreenHeader) sur Résultats et Scénarios ; le header compact sticky du Vote est conservé (AC #24 l'autorise) | ✅ Conforme |
| Carte héro dorée | Réservée au meilleur créneau et à la date confirmée ; bordure `warning 30%`, 2pt | ✅ Conforme (voir A1) |
| Bleu d'action | Un seul token `#2563EB` (CTA primaire, texte secondaire, icônes) ; `permissionBlue #3F8FF2` retiré | ✅ Conforme |
| Chips méta | 30pt capsule, icône + caption, fond subtil | ✅ Conforme |
| Scénarios | Dégradé bleu hero supprimé ; badge de statut conservé en chip accent | ✅ Conforme |

## Accessibilité — contrastes mesurés (WCAG 2.1)

### Passés (6/8 vérifications)

| Pair | Ratio | Seuil |
|---|---|---|
| Texte primaire `#17171F` sur ivoire | 15.85 | 4.5 ✅ |
| Texte secondaire `#606576` sur ivoire | 5.16 | 4.5 ✅ |
| Texte primaire/secondaire sur carte blanche | 17.82 / 5.80 | 4.5 ✅ |
| Label blanc sur CTA `#2563EB` | 5.17 | 4.5 ✅ |
| Texte secondaire sur chip grise | 5.20 | 4.5 ✅ |
| CTA bleu vs ivoire (non-texte) | 4.60 | 3.0 ✅ |

### A1 — Bordure dorée du héro : 1.36 vs 3.0 (non-texte) — **qualifier, ne pas corriger aveuglément**

La bordure `warning #D97706` à 30% sur ivoire ne peut pas atteindre 3:1, même
à 65% d'opacité (1.95). **Recommandation : qualifier la bordure de décorative**
au sens WCAG 1.4.11 : le signal « info confirmée » est porté de manière
redondante par l'étoile amber pleine + le libellé explicite (« Meilleur
créneau », « Date confirmée »). Condition d'acceptation : ce double marqueur
doit rester présent partout où `WakeveHeroCard` est utilisé — à verrouiller
par une note dans la doc du composant (déjà amorcée : « Ne pas multiplier »).

### A2 — Chip de statut Scénarios (phaseBadge) : 3.81 vs 4.5 — **correction requise**

Texte `#2563EB` sur fond accent 14% : sous AA pour du texte callout (~16pt,
non-« large »). Options mesurées :
- texte `#1D4ED8` (blue-700) sur accent 14% → **4.94 ✅** (choix recommandé,
  conserve la teinte bleue)
- texte `textPrimary`/`midnightBlue` → 13.1–13.7 ✅ (moins nuancé)
- augmenter l'opacité du fond dégrade le ratio (3.51 à 20%) ❌

Correctif : foreground du `phaseBadge` → `#1D4ED8`. Effort : 1 ligne.

### Touch targets & Dynamic Type

- Boutons circulaires 44pt (`WakeveCircleButton`), 46pt (`SecondaryIconButton`)
  : ✅ ≥ 44pt.
- Chips méta 30pt : non interactives (information), pas d'exigence de cible.
- Typographie : `Font.callout`/`.caption` système → Dynamic Type ✅.
- `informationCard` gère `reduceTransparency` et `increased contrast` ✅.

## A3 — Arbitrage studio (demandé)

**Constat corrigé** : `EventCreationStudioView` n'est **pas un sheet modal**.
C'est un écran plein écran du routeur d'expérience invitation (présenté inline
dans `ContentView`, `case .eventCreation`), sans fond de page explicite → il
hérite du `systemBackground` blanc. Ma première qualification (« sheet natif
assumé ») était erronée.

**Arbitrage recommandé : appliquer le fond ivoire standard.** Raisons :
1. L'AC #27 vise « plus aucun fond `#FFFFFF` sur les parcours principaux » —
   la création d'événement est l'entrée du parcours.
2. Aucune sémantique modale ne justifie une rupture visuelle (pas de
   `presentSheet`, pas de grabber).
3. La continuité de marque préparer→décider→coordonner inclut la préparation
   (gate Product Excellence).

Le fond système natif reste légitime uniquement pour les vrais sheets modaux
(ex. `CreateEventSheet`, confirmations) : démarcation modale = convention iOS.

## Findings et actions

| # | Sévérité | Finding | Action | Effort |
|---|---|---|---|---|
| A1 | Mineure (qualifiée) | Bordure dorée 1.36 vs 3.0 non-texte | Qualifier décorative + doc composant (redondance étoile + libellé obligatoire) | S |
| A2 | Majeure | phaseBadge 3.81 < 4.5 AA | Foreground → `#1D4ED8` | XS |
| A3 | Majeure | Studio plein écran sur fond système | Appliquer `WakeveScreenBackground(style: .app)` | XS |
| A4 | Note | Chips caption 12pt : lisible mais petit | Surveiller en test utilisateur ; aucun défaut WCAG | — |

## Résidual risk

- Dark mode non couvert par ce cycle (captures light uniquement) : le héro
  doré et la chip accent en dark (`warning.subtle`, opacité 0.22) restent à
  mesurer au prochain cycle.
- La validation finale du libellé d'écran Vote (header compact) et des
  8 écrans sous Dynamic Type XL reste à faire sur device physique.
