# Refonte iOS — Couche 3 (accueil `EventsHomeView`) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sous le flag `iosRedesign2026`, remplacer la racine de la zone Événements par un nouvel accueil : carte « Prochaine étape » (action la plus urgente tous événements confondus), grille 2 colonnes de cartes d'événement avec statut coloré et avatars, section « Passés » repliée, menu contextuel, état vide et bandeau de synchro discret.

**Architecture :**
1. **Cœur pur** (`Models/Home/HomeEventSummary.swift`) : à partir de faits sur un événement (`HomeEventFacts`), calcule statut `WK.Status`, libellé, rang de tri et candidat « Prochaine étape ». 100 % testable sans rendu.
2. **Chargement** (`ViewModels/EventsHomeViewModel.swift`, `@MainActor ObservableObject`) : assemble les `HomeEventFacts` via les projections de la bibliothèque (filtrage par utilisateur, rôle, passé/à venir, synchro) et le dépôt d'événements (votes, bulletin complet de l'utilisateur, noms des participants). Rechargé à l'apparition et quand la zone Événements redevient active.
3. **Vue** (`Views/Home/EventsHomeView.swift`) : composants `WK` uniquement ; aucun style en dur.
4. **Branchement** : nouvelle branche `if iosRedesign2026` en tête de `invitationExperienceRootContent` (`ContentView.swift`) ; les vues legacy (`EventListView`, `EventCard`, `EventLibraryView`) restent intactes pour le chemin flag éteint et les tests qui les lisent.

**Tech Stack:** SwiftUI (iOS 18.2 min), module Kotlin `Shared` (`import Shared`), XCTest. Dossiers Xcode synchronisés.

**Spec :** `docs/superpowers/specs/2026-09-28-ios-redesign-design.md` §5.1, §8 — **Proposition :** Swarm DAO #47.

**Écarts assumés vs spec (reportés dans la spec en Task 8) :**
- Pas d'action « Relancer » (aucune API de relance) : l'action de la carte « Prochaine étape » ouvre l'écran pertinent (voter, résultats, organisation).
- Menu contextuel : Ouvrir, Modifier (brouillons), Supprimer (organisateur, non finalisé). Pas de « Dupliquer » ni « Archiver » (aucune API).
- Le titre « wakeve » de l'en-tête du shell est ajouté au centre de `RedesignShellView` dans cette couche.

---

## Contraintes et commandes

- Worktree : `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Ne jamais indexer `.dao/*`. **Aucune ligne d'attribution dans les commits.** Français **au tutoiement**.
- Test d'une classe : `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -only-testing:WakeveTests/<Classe> 2>&1 | tail -30`
- Suite complète : ajouter `-parallel-testing-enabled NO` (l'exécution parallèle plante par intermittence sur les clones du simulateur). Baseline : seuls 5 échecs préexistants dans `InvitationExperienceRuntimeSurfaceTests`.
- Ne jamais toucher au simulateur « Wakeve-QA-iPhone-16-Pro ». Jamais deux `xcodebuild` en parallèle.
- **Ne pas modifier/déplacer** : `struct EventListView`, `struct EventCard`, `EventLibraryView.swift`, la tranche `private var invitationExperienceRootContent` … `// MARK: - Tab Content` doit continuer de contenir `invitationExperienceRolloutEnabled` et `EventLibraryView(`, `homeTabContent` et ses `case`.
- Pièges du pont Kotlin : `Event` = `WakeveEvent` ; description = `event.description_` (**jamais** `event.description`, qui est le dump de débogage) ; `EventStatus` est une enum Kotlin → `switch` avec `default:` ; dates en `String` ISO 8601 (UTC).
- Garde-fou `WKStyleGuardTests` : aucun `Color(hex:`, `.font(.system(size:`, `cornerRadius:` numérique, `.cornerRadius(`, `Color(red:` dans les nouveaux fichiers (le cliquet de `Views/` ne doit pas monter).

## Structure des fichiers

| Fichier | Action | Responsabilité |
|---|---|---|
| `iosApp/src/Models/Home/HomeEventSummary.swift` | Créer | `HomeEventFacts`, `HomeEventSummary`, `HomeNextStep`, règles pures |
| `iosApp/src/Models/Home/HomeDateText.swift` | Créer | Libellés de date courts et « dans N j » (purs, locale injectable) |
| `iosApp/src/ViewModels/EventsHomeViewModel.swift` | Créer | Chargement des faits, sections, rechargement |
| `iosApp/src/Views/Home/EventsHomeView.swift` | Créer | Vue d'accueil |
| `iosApp/src/Views/Home/HomeEventCard.swift` | Créer | Carte d'événement de la grille |
| `iosApp/src/Views/App/RedesignShellView.swift` | Modifier | Mot-symbole « wakeve » au centre de l'en-tête |
| `iosApp/src/Views/App/ContentView.swift` | Modifier | Branche `iosRedesign2026` dans `invitationExperienceRootContent` |
| `iosApp/src/Resources/{en,fr,es,it,pt}.lproj/Localizable.strings` (+ `.stringsdict`) | Modifier | Clés `home.v2.*` |
| `iosApp/WakeveTests/HomeEventSummaryTests.swift`, `HomeDateTextTests.swift`, `EventsHomeViewModelTests.swift`, `EventsHomeViewTests.swift` | Créer | |

---

### Task 1 : Cœur pur — `HomeEventFacts` → `HomeEventSummary`

**Files:** Create `iosApp/src/Models/Home/HomeEventSummary.swift` ; Test `iosApp/WakeveTests/HomeEventSummaryTests.swift`

Règles (les « faits » sont déjà calculés ; ce fichier ne dépend ni de `Shared` ni de SwiftUI, sauf `WK.Status`) :

| Situation | `status` | `labelKey` | Rang de tri |
|---|---|---|---|
| Brouillon, organisateur | `.draft` | `home.v2.status.draft` (« Brouillon ») | 3 |
| Sondage, l'utilisateur doit voter (participant accepté ou organisateur votant, bulletin incomplet) | `.actionNeeded` | `home.v2.status.vote_required` (« À toi de voter ») | 0 |
| Sondage, tous les votants ont voté, organisateur | `.actionNeeded` | `home.v2.status.ready_to_confirm` (« Prêt à confirmer ») | 0 |
| Sondage, autres cas | `.pending` | `home.v2.status.polling` (« Vote en cours ») | 1 |
| Comparaison / confirmé / organisation, organisateur | `.pending` | `home.v2.status.organizing` (« En préparation ») | 1 |
| Confirmé / organisation / finalisé à venir, avec date | `.confirmed` | date courte (ex. « Sam. 12 oct ») | 2 |
| Passé (quel que soit le statut) | `.draft` (neutre) | date courte ou `home.v2.status.past` | 4 (section Passés) |

À rang égal : échéance ou date la plus proche d'abord, puis titre.

« Prochaine étape » (au plus une, la plus urgente parmi les non-passés) :
1. `voteRequired` de l'échéance la plus proche → `caption` « Prochaine étape · <titre> », `value` = votes reçus, `unit` = « /<votants> », `subtitle` = « Il manque ton vote · clôture dans N j » (ou sans échéance : « Il manque ton vote »), action « Voter » → `.vote`.
2. `readyToConfirm` → `value`/`unit` = votes, `subtitle` « Tout le monde a voté », action « Choisir la date » → `.pollResults`.
3. Organisateur, sondage en cours → `subtitle` « votes reçus · clôture dans N j », action « Voir les résultats » → `.pollResults`.
4. Organisateur, organisation → `value` = jours avant l'événement (si date), `unit` « j », `subtitle` « avant l'événement », action « Continuer l'organisation » → `.open`.
5. Sinon `nil` (la carte est masquée).

- [ ] **Step 1 : Tests**

```swift
import XCTest
@testable import Wakeve

final class HomeEventSummaryTests: XCTestCase {

    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func facts(
        id: String = "e1",
        title: String = "Week-end Annecy",
        phase: HomeEventFacts.Phase = .polling,
        role: HomeEventFacts.Role = .participant,
        isPast: Bool = false,
        userBallotComplete: Bool = false,
        votersWithCompleteBallot: Int = 5,
        eligibleVoters: Int = 8,
        deadline: Date? = nil,
        eventDate: Date? = nil
    ) -> HomeEventFacts {
        HomeEventFacts(
            id: id, title: title, phase: phase, role: role, isPast: isPast,
            userBallotComplete: userBallotComplete,
            votersWithCompleteBallot: votersWithCompleteBallot, eligibleVoters: eligibleVoters,
            deadline: deadline, eventDate: eventDate, participantNames: ["Léa", "Tom"]
        )
    }

    func testParticipantWhoHasNotVotedMustAct() {
        let s = HomeEventSummary(facts: facts(), now: now)
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.vote_required"))
        XCTAssertEqual(s.sortRank, 0)
    }

    func testParticipantWhoVotedWaits() {
        let s = HomeEventSummary(facts: facts(userBallotComplete: true), now: now)
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.polling"))
        XCTAssertEqual(s.sortRank, 1)
    }

    func testOrganizerIsToldWhenEveryoneVoted() {
        let s = HomeEventSummary(
            facts: facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8),
            now: now
        )
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.ready_to_confirm"))
    }

    func testDraftIsNeutralAndLast() {
        let s = HomeEventSummary(facts: facts(phase: .draft, role: .organizer), now: now)
        XCTAssertEqual(s.status, .draft)
        XCTAssertEqual(s.sortRank, 3)
    }

    func testConfirmedUpcomingShowsItsDate() {
        let date = ISO8601DateFormatter().date(from: "2026-10-12T18:00:00Z")!
        let s = HomeEventSummary(facts: facts(phase: .confirmed, role: .participant, eventDate: date), now: now)
        XCTAssertEqual(s.status, .confirmed)
        XCTAssertEqual(s.label, .date(date))
        XCTAssertEqual(s.sortRank, 2)
    }

    func testOrganizerOrganizingIsPending() {
        let s = HomeEventSummary(facts: facts(phase: .organizing, role: .organizer), now: now)
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.organizing"))
    }

    func testPastEventsGoToThePastSection() {
        let s = HomeEventSummary(facts: facts(phase: .finalized, isPast: true), now: now)
        XCTAssertTrue(s.isPast)
        XCTAssertEqual(s.sortRank, 4)
    }

    func testSortPutsActionFirstThenSoonestDeadline() {
        let soon = now.addingTimeInterval(86_400)
        let later = now.addingTimeInterval(5 * 86_400)
        let items = [
            HomeEventSummary(facts: facts(id: "draft", phase: .draft, role: .organizer), now: now),
            HomeEventSummary(facts: facts(id: "voteLater", deadline: later), now: now),
            HomeEventSummary(facts: facts(id: "waiting", userBallotComplete: true), now: now),
            HomeEventSummary(facts: facts(id: "voteSoon", deadline: soon), now: now)
        ]
        XCTAssertEqual(HomeEventSummary.sorted(items).map(\.id), ["voteSoon", "voteLater", "waiting", "draft"])
    }

    func testNextStepPrefersTheMostUrgentVote() {
        let soon = now.addingTimeInterval(2 * 86_400)
        let items = [
            facts(id: "org", role: .organizer, userBallotComplete: true),
            facts(id: "vote", title: "Raclette", deadline: soon)
        ]
        let step = HomeNextStep.pick(from: items, now: now)
        XCTAssertEqual(step?.eventId, "vote")
        XCTAssertEqual(step?.action, .vote)
        XCTAssertEqual(step?.value, "5")
        XCTAssertEqual(step?.unit, "/8")
        XCTAssertEqual(step?.daysLeft, 2)
    }

    func testNextStepForOrganizerWhenEveryoneVoted() {
        let step = HomeNextStep.pick(
            from: [facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8)],
            now: now
        )
        XCTAssertEqual(step?.action, .pollResults)
        XCTAssertEqual(step?.kind, .readyToConfirm)
    }

    func testNoNextStepWhenNothingToDo() {
        XCTAssertNil(HomeNextStep.pick(from: [facts(userBallotComplete: true)], now: now))
        XCTAssertNil(HomeNextStep.pick(from: [facts(isPast: true)], now: now))
    }
}
```

- [ ] **Step 2 : Lancer** `-only-testing:WakeveTests/HomeEventSummaryTests` → BUILD FAILED.

- [ ] **Step 3 : Implémenter**

```swift
import Foundation

/// Faits calculés pour un événement, vus par l'utilisateur courant (couche 3, #47).
struct HomeEventFacts: Equatable {
    enum Phase: Equatable { case draft, polling, comparing, confirmed, organizing, finalized }
    enum Role: Equatable { case organizer, participant }

    let id: String
    let title: String
    let phase: Phase
    let role: Role
    let isPast: Bool
    let userBallotComplete: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let deadline: Date?
    let eventDate: Date?
    let participantNames: [String]

    var everyoneVoted: Bool { eligibleVoters > 0 && votersWithCompleteBallot >= eligibleVoters }
    var voteRequired: Bool { phase == .polling && !isPast && !userBallotComplete }
    var readyToConfirm: Bool { phase == .polling && !isPast && role == .organizer && everyoneVoted }
}

/// Résumé affichable d'une carte d'événement.
struct HomeEventSummary: Identifiable, Equatable {
    enum Label: Equatable { case key(String), date(Date) }

    let facts: HomeEventFacts
    let status: WK.Status
    let label: Label
    let sortRank: Int

    var id: String { facts.id }
    var isPast: Bool { facts.isPast }

    init(facts: HomeEventFacts, now: Date) {
        self.facts = facts
        if facts.isPast {
            status = .draft
            label = facts.eventDate.map(Label.date) ?? .key("home.v2.status.past")
            sortRank = 4
            return
        }
        switch facts.phase {
        case .draft:
            status = .draft; label = .key("home.v2.status.draft"); sortRank = 3
        case .polling:
            if facts.voteRequired {
                status = .actionNeeded; label = .key("home.v2.status.vote_required"); sortRank = 0
            } else if facts.readyToConfirm {
                status = .actionNeeded; label = .key("home.v2.status.ready_to_confirm"); sortRank = 0
            } else {
                status = .pending; label = .key("home.v2.status.polling"); sortRank = 1
            }
        case .comparing, .confirmed, .organizing, .finalized:
            if facts.role == .organizer && facts.phase != .finalized {
                status = .pending; label = .key("home.v2.status.organizing"); sortRank = 1
            } else {
                status = .confirmed
                label = facts.eventDate.map(Label.date) ?? .key("home.v2.status.confirmed")
                sortRank = 2
            }
        }
    }

    /// Tri : rang, puis échéance/date la plus proche, puis titre.
    static func sorted(_ items: [HomeEventSummary]) -> [HomeEventSummary] {
        items.sorted { a, b in
            if a.sortRank != b.sortRank { return a.sortRank < b.sortRank }
            let da = a.facts.deadline ?? a.facts.eventDate ?? .distantFuture
            let db = b.facts.deadline ?? b.facts.eventDate ?? .distantFuture
            if da != db { return da < db }
            return a.facts.title.localizedCompare(b.facts.title) == .orderedAscending
        }
    }
}

/// Carte « Prochaine étape » : l'action la plus urgente tous événements confondus.
struct HomeNextStep: Equatable {
    enum Kind: Equatable { case voteRequired, readyToConfirm, pollInProgress, organizing }
    enum Action: Equatable { case vote, pollResults, open }

    let eventId: String
    let title: String
    let kind: Kind
    let action: Action
    let value: String
    let unit: String?
    let daysLeft: Int?

    static func pick(from facts: [HomeEventFacts], now: Date) -> HomeNextStep? {
        let active = facts.filter { !$0.isPast }
        func soonest(_ items: [HomeEventFacts], by date: (HomeEventFacts) -> Date?) -> HomeEventFacts? {
            items.min { (date($0) ?? .distantFuture) < (date($1) ?? .distantFuture) }
        }
        if let f = soonest(active.filter(\.voteRequired), by: \.deadline) {
            return votes(f, kind: .voteRequired, action: .vote, now: now)
        }
        if let f = soonest(active.filter(\.readyToConfirm), by: \.deadline) {
            return votes(f, kind: .readyToConfirm, action: .pollResults, now: now)
        }
        if let f = soonest(active.filter { $0.phase == .polling && $0.role == .organizer }, by: \.deadline) {
            return votes(f, kind: .pollInProgress, action: .pollResults, now: now)
        }
        let organizing = active.filter {
            $0.role == .organizer && [.comparing, .confirmed, .organizing].contains($0.phase)
        }
        if let f = soonest(organizing, by: \.eventDate) {
            let days = f.eventDate.map { HomeDateText.daysBetween(now, $0) }
            return HomeNextStep(
                eventId: f.id, title: f.title, kind: .organizing, action: .open,
                value: days.map(String.init) ?? "—", unit: days == nil ? nil : "j", daysLeft: days
            )
        }
        return nil
    }

    private static func votes(_ f: HomeEventFacts, kind: Kind, action: Action, now: Date) -> HomeNextStep {
        HomeNextStep(
            eventId: f.id, title: f.title, kind: kind, action: action,
            value: String(f.votersWithCompleteBallot), unit: "/\(f.eligibleVoters)",
            daysLeft: f.deadline.map { HomeDateText.daysBetween(now, $0) }
        )
    }
}
```

`HomeDateText.daysBetween` est créé en Task 2 : pour que la Task 1 compile seule, créer dès maintenant `iosApp/src/Models/Home/HomeDateText.swift` avec uniquement :

```swift
import Foundation

enum HomeDateText {
    /// Nombre de jours calendaires (≥ 0) entre deux instants, dans le calendrier donné.
    static func daysBetween(_ from: Date, _ to: Date, calendar: Calendar = .current) -> Int {
        let start = calendar.startOfDay(for: from)
        let end = calendar.startOfDay(for: to)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }
}
```

- [ ] **Step 4 : Lancer** → PASS (11 tests). Note : `testNextStepPrefersTheMostUrgentVote` suppose le calendrier du simulateur en Europe/UTC±2 ; si `daysLeft` diffère d'un jour à cause du fuseau, injecter `Calendar(identifier: .gregorian)` avec `timeZone = UTC` dans le test via un paramètre `calendar:` ajouté à `pick` (défaut `.current`) plutôt que de changer l'attendu.

- [ ] **Step 5 : Commit** — `feat(ios): add pure home summary and next-step rules` + corps `Refs Swarm DAO #47 (layer 3).`

### Task 2 : Libellés de date et clés localisées

**Files:** Modify `iosApp/src/Models/Home/HomeDateText.swift` ; Modify les 5 `Localizable.strings` et `Localizable.stringsdict` ; Test `iosApp/WakeveTests/HomeDateTextTests.swift` ; ajouter les clés à `WKComponentsContractTests.requiredKeys` ou créer un test équivalent dans `HomeDateTextTests`.

- [ ] **Step 1 : Tests**

```swift
import XCTest
@testable import Wakeve

final class HomeDateTextTests: XCTestCase {
    private var utc: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }

    func testDaysBetweenCountsCalendarDays() {
        let f = ISO8601DateFormatter()
        XCTAssertEqual(HomeDateText.daysBetween(f.date(from: "2026-10-01T23:00:00Z")!, f.date(from: "2026-10-02T01:00:00Z")!, calendar: utc), 1)
        XCTAssertEqual(HomeDateText.daysBetween(f.date(from: "2026-10-03T00:00:00Z")!, f.date(from: "2026-10-01T00:00:00Z")!, calendar: utc), 0)
    }

    func testShortDateIsLocalized() {
        let date = ISO8601DateFormatter().date(from: "2026-10-10T12:00:00Z")!
        let fr = HomeDateText.short(date, locale: Locale(identifier: "fr_FR"), calendar: utc)
        XCTAssertTrue(fr.lowercased().contains("10"), fr)
        XCTAssertTrue(fr.lowercased().contains("oct"), fr)
    }

    func testParsesKotlinIsoStrings() {
        XCTAssertNotNil(HomeDateText.parseISO("2026-10-10T12:00:00Z"))
        XCTAssertNotNil(HomeDateText.parseISO("2026-10-10T12:00:00.123Z"))
        XCTAssertNil(HomeDateText.parseISO(""))
    }

    func testHomeKeysExistInEveryLocale() throws {
        let keys = [
            "home.v2.status.draft", "home.v2.status.vote_required", "home.v2.status.ready_to_confirm",
            "home.v2.status.polling", "home.v2.status.organizing", "home.v2.status.confirmed", "home.v2.status.past",
            "home.v2.next_step.caption_format", "home.v2.next_step.vote.subtitle", "home.v2.next_step.ready.subtitle",
            "home.v2.next_step.polling.subtitle", "home.v2.next_step.organizing.subtitle",
            "home.v2.next_step.action.vote", "home.v2.next_step.action.results", "home.v2.next_step.action.organize",
            "home.v2.section.past", "home.v2.empty.title", "home.v2.empty.body", "home.v2.empty.action",
            "home.v2.menu.open", "home.v2.menu.edit", "home.v2.menu.delete", "home.v2.sync.pending", "wk.wordmark"
        ]
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in keys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            XCTAssertTrue(dict.contains("<key>home.v2.closes_in_days</key>"), "pluriel manquant (\(locale))")
        }
    }
}
```

- [ ] **Step 2 : Lancer** → FAIL.

- [ ] **Step 3 : Implémenter** — compléter `HomeDateText` :

```swift
    static func short(_ date: Date, locale: Locale = WK.appLocale, calendar: Calendar = .current) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.setLocalizedDateFormatFromTemplate("EEE d MMM")
        return formatter.string(from: date)
    }

    static func parseISO(_ value: String?) -> Date? {
        guard let value, !value.isEmpty else { return nil }
        let withFraction = ISO8601DateFormatter()
        withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return withFraction.date(from: value) ?? ISO8601DateFormatter().date(from: value)
    }

    /// « clôture dans N j » avec pluriel (stringsdict `home.v2.closes_in_days`).
    static func closesIn(days: Int, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("home.v2.closes_in_days", locale: locale), locale: locale, days)
    }
```

(Vérifier la signature réelle de `WK.localizedFormat` / `WK.appLocale` dans `Theme/WK.swift`.)

Ajouter les clés (fin de fichier, bloc `/* Home v2 (refonte 2026) */`). Français **au tutoiement** :

| Clé | en | fr | es | it | pt |
|---|---|---|---|---|---|
| home.v2.status.draft | Draft | Brouillon | Borrador | Bozza | Rascunho |
| home.v2.status.vote_required | Your vote | À toi de voter | Te toca votar | Tocca a te votare | Sua vez de votar |
| home.v2.status.ready_to_confirm | Ready to confirm | Prêt à confirmer | Listo para confirmar | Pronto da confermare | Pronto para confirmar |
| home.v2.status.polling | Voting | Vote en cours | Votación en curso | Voto in corso | Votação em andamento |
| home.v2.status.organizing | In progress | En préparation | En preparación | In preparazione | Em preparação |
| home.v2.status.confirmed | Confirmed | Confirmé | Confirmado | Confermato | Confirmado |
| home.v2.status.past | Past | Passé | Pasado | Passato | Passado |
| home.v2.next_step.caption_format | Next step · %@ | Prochaine étape · %@ | Próximo paso · %@ | Prossimo passo · %@ | Próximo passo · %@ |
| home.v2.next_step.vote.subtitle | Your vote is missing | Il manque ton vote | Falta tu voto | Manca il tuo voto | Falta o seu voto |
| home.v2.next_step.ready.subtitle | Everyone has voted | Tout le monde a voté | Todos han votado | Hanno votato tutti | Todos votaram |
| home.v2.next_step.polling.subtitle | votes received | votes reçus | votos recibidos | voti ricevuti | votos recebidos |
| home.v2.next_step.organizing.subtitle | days to go | avant l'événement | días para el evento | giorni all'evento | dias para o evento |
| home.v2.next_step.action.vote | Vote | Voter | Votar | Vota | Votar |
| home.v2.next_step.action.results | See results | Voir les résultats | Ver resultados | Vedi risultati | Ver resultados |
| home.v2.next_step.action.organize | Keep organizing | Continuer l'organisation | Seguir organizando | Continua a organizzare | Continuar organizando |
| home.v2.section.past | Past | Passés | Pasados | Passati | Passados |
| home.v2.empty.title | Plan your first event | Organise ton premier événement | Organiza tu primer evento | Organizza il tuo primo evento | Organize seu primeiro evento |
| home.v2.empty.body | Propose dates, the group votes, Wakeve keeps track. | Propose des dates, le groupe vote, Wakeve suit le reste. | Propón fechas, el grupo vota y Wakeve se encarga del resto. | Proponi le date, il gruppo vota, Wakeve pensa al resto. | Proponha datas, o grupo vota e o Wakeve cuida do resto. |
| home.v2.empty.action | Create an event | Créer un événement | Crear un evento | Crea un evento | Criar um evento |
| home.v2.menu.open | Open | Ouvrir | Abrir | Apri | Abrir |
| home.v2.menu.edit | Edit draft | Modifier le brouillon | Editar borrador | Modifica bozza | Editar rascunho |
| home.v2.menu.delete | Delete | Supprimer | Eliminar | Elimina | Excluir |
| home.v2.sync.pending | Changes waiting to sync | Modifications en attente de synchro | Cambios pendientes de sincronizar | Modifiche in attesa di sincronizzazione | Alterações aguardando sincronização |
| wk.wordmark | wakeve | wakeve | wakeve | wakeve | wakeve |

`Localizable.stringsdict` (5 langues), clé `home.v2.closes_in_days`, format `%#@days@`, règle plurielle `d` : en one « closes in %d day » / other « closes in %d days » ; fr one « clôture dans %d j » / other « clôture dans %d j » ; es « cierra en %d día » / « cierra en %d días » ; it « chiude tra %d giorno » / « chiude tra %d giorni » ; pt « fecha em %d dia » / « fecha em %d dias ». `plutil -lint` sur les 10 fichiers.

- [ ] **Step 4 : Lancer** `HomeDateTextTests` + `WKComponentsContractTests` → PASS.
- [ ] **Step 5 : Commit** — `feat(ios): add home date helpers and localized copy` + `Refs Swarm DAO #47 (layer 3).`

### Task 3 : `EventsHomeViewModel` (chargement des faits)

**Files:** Create `iosApp/src/ViewModels/EventsHomeViewModel.swift` ; Test `iosApp/WakeveTests/EventsHomeViewModelTests.swift`

Conception : séparer la **source** (protocole injectable) de l'assemblage, pour tester sans base de données.

```swift
import Foundation

/// Données brutes nécessaires à l'accueil, fournies par une source injectable.
struct HomeRawEvent: Equatable {
    let id: String
    let title: String
    let statusName: String          // `EventStatus.name` Kotlin : "DRAFT", "POLLING", …
    let isOrganizer: Bool
    let isPast: Bool                // depuis LibraryCardProjection.temporalClass
    let deadlineISO: String
    let finalDateISO: String?
    let firstSlotStartISO: String?
    let userBallotComplete: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let participantNames: [String]
    let hasPendingSync: Bool
}

protocol EventsHomeSource {
    func loadEvents(viewerId: String) async throws -> [HomeRawEvent]
}

@MainActor
final class EventsHomeViewModel: ObservableObject {
    enum State: Equatable { case loading, empty, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var active: [HomeEventSummary] = []
    @Published private(set) var past: [HomeEventSummary] = []
    @Published private(set) var nextStep: HomeNextStep?
    @Published private(set) var pendingSyncCount = 0

    private let viewerId: String
    private let source: EventsHomeSource
    private let now: () -> Date

    init(viewerId: String, source: EventsHomeSource, now: @escaping () -> Date = Date.init) {
        self.viewerId = viewerId
        self.source = source
        self.now = now
    }

    func reload() async {
        do {
            let raw = try await source.loadEvents(viewerId: viewerId)
            let current = now()
            let facts = raw.map { Self.facts(from: $0) }
            let summaries = HomeEventSummary.sorted(facts.map { HomeEventSummary(facts: $0, now: current) })
            active = summaries.filter { !$0.isPast }
            past = summaries.filter(\.isPast)
            nextStep = HomeNextStep.pick(from: facts, now: current)
            pendingSyncCount = raw.filter(\.hasPendingSync).count
            state = raw.isEmpty ? .empty : .loaded
        } catch {
            state = active.isEmpty && past.isEmpty ? .failed : .loaded
        }
    }

    static func facts(from raw: HomeRawEvent) -> HomeEventFacts {
        let phase: HomeEventFacts.Phase
        switch raw.statusName {
        case "DRAFT": phase = .draft
        case "POLLING": phase = .polling
        case "COMPARING": phase = .comparing
        case "CONFIRMED": phase = .confirmed
        case "ORGANIZING": phase = .organizing
        default: phase = .finalized
        }
        return HomeEventFacts(
            id: raw.id, title: raw.title, phase: phase,
            role: raw.isOrganizer ? .organizer : .participant, isPast: raw.isPast,
            userBallotComplete: raw.userBallotComplete,
            votersWithCompleteBallot: raw.votersWithCompleteBallot, eligibleVoters: raw.eligibleVoters,
            deadline: HomeDateText.parseISO(raw.deadlineISO),
            eventDate: HomeDateText.parseISO(raw.finalDateISO) ?? HomeDateText.parseISO(raw.firstSlotStartISO),
            participantNames: raw.participantNames
        )
    }
}
```

- [ ] **Step 1 : Tests** (source factice) — états `empty`/`loaded`/`failed`, séparation actifs/passés, tri, `nextStep`, `pendingSyncCount`, conservation des données en cas d'échec d'un rechargement :

```swift
import XCTest
@testable import Wakeve

@MainActor
final class EventsHomeViewModelTests: XCTestCase {

    private struct StubSource: EventsHomeSource {
        var result: Result<[HomeRawEvent], Error>
        func loadEvents(viewerId: String) async throws -> [HomeRawEvent] { try result.get() }
    }
    private struct Boom: Error {}

    private func raw(_ id: String, status: String = "POLLING", organizer: Bool = false, past: Bool = false,
                     voted: Bool = false, pending: Bool = false) -> HomeRawEvent {
        HomeRawEvent(id: id, title: id, statusName: status, isOrganizer: organizer, isPast: past,
                     deadlineISO: "2026-10-05T10:00:00Z", finalDateISO: nil, firstSlotStartISO: nil,
                     userBallotComplete: voted, votersWithCompleteBallot: 3, eligibleVoters: 6,
                     participantNames: ["Léa"], hasPendingSync: pending)
    }

    private let fixedNow = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    func testEmpty() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .success([])), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .empty)
        XCTAssertNil(vm.nextStep)
    }

    func testSplitsSortsAndPicksNextStep() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .success([
            raw("waiting", voted: true), raw("mine"), raw("old", status: "FINALIZED", past: true),
            raw("draft", status: "DRAFT", organizer: true, pending: true)
        ])), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.active.map(\.id), ["mine", "waiting", "draft"])
        XCTAssertEqual(vm.past.map(\.id), ["old"])
        XCTAssertEqual(vm.nextStep?.eventId, "mine")
        XCTAssertEqual(vm.pendingSyncCount, 1)
    }

    func testFailureWithoutDataShowsFailed() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .failure(Boom())), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .failed)
    }

    func testUnknownStatusIsTreatedAsFinalized() {
        XCTAssertEqual(EventsHomeViewModel.facts(from: raw("x", status: "WHATEVER")).phase, .finalized)
    }
}
```

- [ ] **Step 2 : Lancer** → BUILD FAILED. **Step 3 :** implémenter (code ci-dessus). **Step 4 :** PASS (4 tests).

- [ ] **Step 5 : Source réelle** `SharedEventsHomeSource` (même fichier ou `Services/SharedEventsHomeSource.swift`), **non testée unitairement** (dépend de la base), vérifiée en Task 7 :
  - Événements visibles + rôle + passé : `RepositoryProvider.shared` projection repository de la bibliothèque (voir `EventLibraryViewModel.reload()` dans `Views/Invitations/EventLibraryView.swift:81-165` pour l'appel exact `library(viewerId:projection:now:)`, le cast `LibraryLoadStateReady<NSArray>` → `[LibraryCardProjection]`). Appeler les projections `.upcoming`, `.drafts` et `.past`, dédupliquer par `event.id`. `isOrganizer` = `card.memberships` contient `.hosting` **ou** `event.organizerId == viewerId`. `isPast` = `card.temporalClass == .past` **sauf** si `event.status` est `POLLING`/`DRAFT` (le classifieur range en « passé » un sondage sans créneau daté — on le garde actif). `hasPendingSync` = `card.syncState` différent de `Synced`.
  - Votes : `repository.getPoll(eventId:)?.votes` (dictionnaire participant → (slot → vote)) : `votersWithCompleteBallot` = nombre de participants ayant voté pour **tous** les `event.proposedSlots` ; `eligibleVoters` = `event.participants.count` (au moins 1) ; `userBallotComplete` = `repository.hasCompleteBallot(eventId:participantId: viewerId)` (voir `Views/Polls/PollVotingView.swift:76-80`).
  - Noms : `RepositoryProvider.shared.database.userQueries.selectUserById(id:).executeAsOneOrNull()?.name`, repli sur l'identifiant (voir `Views/Invitations/EventAudienceView.swift:69-80`), limiter à 5 noms.
  - Dates : `event.deadline`, `event.finalDate`, `event.proposedSlots.first?.start`.
  - Statut : nom Kotlin de `event.status` (ex. `event.status.name`).
  Tout l'accès Kotlin reste dans cette source ; rien dans la vue.

- [ ] **Step 6 : Commit** — `feat(ios): load home event facts through an injectable source` + `Refs Swarm DAO #47 (layer 3).`

### Task 4 : `HomeEventCard`

**Files:** Create `iosApp/src/Views/Home/HomeEventCard.swift` ; Test `iosApp/WakeveTests/EventsHomeViewTests.swift`

- [ ] **Step 1 : Tests** — libellé VoiceOver et libellé de statut :

```swift
import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class EventsHomeViewTests: XCTestCase {
    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func summary(voted: Bool) -> HomeEventSummary {
        HomeEventSummary(facts: HomeEventFacts(
            id: "e1", title: "Raclette", phase: .polling, role: .participant, isPast: false,
            userBallotComplete: voted, votersWithCompleteBallot: 2, eligibleVoters: 4,
            deadline: nil, eventDate: nil, participantNames: ["Léa", "Tom"]
        ), now: now)
    }

    func testCardAccessibilityLabelReadsTitleStatusAndPeople() {
        let label = HomeEventCard.accessibilityLabel(for: summary(voted: false), locale: Locale(identifier: "en"))
        XCTAssertTrue(label.hasPrefix("Raclette, "))
        XCTAssertTrue(label.contains(WK.Status.actionNeeded.localizedName) || label.contains(String(localized: "home.v2.status.vote_required")))
        XCTAssertTrue(label.contains("Léa"))
    }

    func testCardKeepsMinimumTapTargetAtAX5() {
        let host = UIHostingController(rootView: HomeEventCard(summary: summary(voted: true), onOpen: {})
            .environment(\.dynamicTypeSize, .accessibility5))
        let size = host.sizeThatFits(in: CGSize(width: 180, height: .greatestFiniteMagnitude))
        XCTAssertGreaterThanOrEqual(size.height, WK.Size.minTapTarget)
    }
}
```

- [ ] **Step 2 :** Lancer → BUILD FAILED.

- [ ] **Step 3 : Implémenter** — `HomeEventCard(summary:onOpen:)` : `Button` contenant `WKCard` { `WKAvatarStack` (noms → `WKAvatar(id: "\(summary.id)-\(index)", name:)`), titre (`WK.Typo.headline`, `lineLimit(2)`, sans limite aux tailles d'accessibilité), `WKStatusPill(text: statusText, status: summary.status)` } ; `statusText` : `.key(k)` → `String(localized: String.LocalizationValue(k))`, `.date(d)` → `HomeDateText.short(d)`. `static func accessibilityLabel(for:locale:)` = « titre, statut, noms » (noms via `WKAvatarStack.accessibilityLabel(for:locale:)`). `.accessibilityElement(children: .ignore)` **n'est pas** utilisé sur le `Button` (garder les traits natifs) : poser `.accessibilityLabel(...)` sur le bouton. Identifiant `wkAccessibilityID("home.card.\(summary.id)")`.

- [ ] **Step 4 :** PASS. **Step 5 : Commit** — `feat(ios): add home event card` + `Refs Swarm DAO #47 (layer 3).`

### Task 5 : `EventsHomeView`

**Files:** Create `iosApp/src/Views/Home/EventsHomeView.swift` ; Test : ajouter à `EventsHomeViewTests`.

- [ ] **Step 1 : Tests**

```swift
    func testHomeShowsEmptyStateAndHidesHeroWhenNoEvents() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/Home/EventsHomeView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("case .empty"))
        XCTAssertTrue(source.contains("WKHeroMetric("))
        XCTAssertTrue(source.contains("if let step = viewModel.nextStep"))
        XCTAssertTrue(source.contains("DisclosureGroup"), "La section Passés est repliée par défaut.")
        XCTAssertTrue(source.contains(".contextMenu"))
        XCTAssertTrue(source.contains(".refreshable"))
    }

    func testGridUsesOneColumnAtAccessibilitySizes() {
        XCTAssertEqual(EventsHomeView.columnCount(for: .large), 2)
        XCTAssertEqual(EventsHomeView.columnCount(for: .accessibility1), 1)
    }

    func testNextStepSubtitleCombinesMissingVoteAndDeadline() {
        let step = HomeNextStep(eventId: "e", title: "Raclette", kind: .voteRequired, action: .vote,
                                value: "5", unit: "/8", daysLeft: 2)
        let text = EventsHomeView.subtitle(for: step, locale: Locale(identifier: "en"))
        XCTAssertTrue(text.contains("2"), text)
    }
```

- [ ] **Step 2 :** Lancer → BUILD FAILED.

- [ ] **Step 3 : Implémenter** `EventsHomeView` :

```swift
struct EventsHomeView: View {
    @ObservedObject var viewModel: EventsHomeViewModel
    let onOpenEvent: (String) -> Void
    let onNextStep: (HomeNextStep) -> Void
    let onCreate: () -> Void
    let canDelete: (String) -> Bool
    let onEditDraft: (String) -> Void
    let onDelete: (String) -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var showsPast = false

    static func columnCount(for size: DynamicTypeSize) -> Int { size.isAccessibilitySize ? 1 : 2 }

    static func subtitle(for step: HomeNextStep, locale: Locale = WK.appLocale) -> String { … }
    // voteRequired : "home.v2.next_step.vote.subtitle" + (daysLeft → " · " + HomeDateText.closesIn(days:))
    // readyToConfirm : "home.v2.next_step.ready.subtitle"
    // pollInProgress : "home.v2.next_step.polling.subtitle" + (daysLeft → " · " + closesIn)
    // organizing : "home.v2.next_step.organizing.subtitle"
    // (clés résolues avec WK.localizedFormat(_, locale:) pour respecter `locale`)

    var body: some View { … }
}
```

Corps : `ScrollView` → `LazyVStack(spacing: WK.Space.md)` :
- bandeau discret si `viewModel.pendingSyncCount > 0` : ligne `WK.Typo.caption`, `WK.Colors.textMuted`, icône `arrow.triangle.2.circlepath`, texte `home.v2.sync.pending` ;
- `if let step = viewModel.nextStep` → `WKHeroMetric(caption: String(format: localized "home.v2.next_step.caption_format", step.title), value: step.value, unit: step.unit, subtitle: Self.subtitle(for: step), actionTitle: <clé action selon step.action>, action: { onNextStep(step) })` ;
- `switch viewModel.state` : `.loading` → `ProgressView()` ; `.empty` → `WKCard` avec titre `home.v2.empty.title`, corps `home.v2.empty.body`, `WKPrimaryButton(title: home.v2.empty.action, action: onCreate, accessibilityID: "home.empty.create")` ; `.failed` → texte d'erreur existant (`events.empty.subtitle` ou équivalent) + bouton réessayer (`Task { await viewModel.reload() }`) ; `.loaded` → grille `LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: WK.Space.sm), count: Self.columnCount(for: dynamicTypeSize)), spacing: WK.Space.sm)` de `HomeEventCard` avec `.contextMenu` (Ouvrir ; Modifier le brouillon si `summary.facts.phase == .draft && summary.facts.role == .organizer` ; Supprimer, rôle destructif, si `canDelete(id)`) ;
- `if !viewModel.past.isEmpty` → `DisclosureGroup(isExpanded: $showsPast) { grille des passés } label: { Text(home.v2.section.past) }`.
- Marges `WK.Space.screen`, fond `WK.Colors.canvas.ignoresSafeArea()`, `.refreshable { await viewModel.reload() }`, `.task { await viewModel.reload() }`.
- Aucun style en dur.

- [ ] **Step 4 :** PASS + `WKStyleGuardTests` PASS. **Step 5 : Commit** — `feat(ios): add EventsHomeView with next step, grid and past section` + `Refs Swarm DAO #47 (layer 3).`

### Task 6 : Branchement dans le shell

**Files:** Modify `iosApp/src/Views/App/ContentView.swift`, `iosApp/src/Views/App/RedesignShellView.swift` ; Test : ajouter à `RedesignShellTests`.

- [ ] **Step 1 : Tests (ajouts)**

```swift
    func testRedesignRootInstallsTheNewHome() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var invitationExperienceRootContent") else { return XCTFail() }
        let slice = String(source[start.lowerBound...].prefix(1500))
        XCTAssertTrue(slice.contains("if iosRedesign2026"), "La nouvelle racine est prioritaire sous le flag.")
        XCTAssertTrue(slice.contains("EventsHomeView("))
        XCTAssertTrue(slice.contains("invitationExperienceRolloutEnabled"), "Le chemin legacy reste en place.")
    }

    func testShellHeaderShowsTheWordmark() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/App/RedesignShellView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("\"wk.wordmark\""))
    }
```
(Réutiliser le helper `contentViewSource()` existant de `RedesignShellTests`.)

- [ ] **Step 2 :** Lancer → FAIL.

- [ ] **Step 3 : Implémenter**
  1. `AuthenticatedView` : `@StateObject private var eventsHomeViewModel: EventsHomeViewModel` — `AuthenticatedView` a un `init` implicite avec `userId` ; si ajouter un `@StateObject` dépendant de `userId` impose un `init` explicite, le faire en initialisant `_eventsHomeViewModel = StateObject(wrappedValue: EventsHomeViewModel(viewerId: userId, source: SharedEventsHomeSource()))` et en conservant tous les autres membres. **Alternative moins intrusive (préférée si l'`init` casse des appels)** : un petit conteneur `EventsHomeContainer(userId:…)` qui possède le `@StateObject` et rend `EventsHomeView`.
  2. En tête de `invitationExperienceRootContent` :
     ```swift
     if iosRedesign2026 {
         EventsHomeContainer(
             userId: userId,
             reloadToken: eventsHomeReloadToken,
             onOpenEvent: { id in openEventFromHome(id) },
             onNextStep: { step in handleHomeNextStep(step) },
             onCreate: { beginRedesignEventCreation() },
             canDelete: { id in canDeleteFromHome(id) },
             onEditDraft: { id in editDraftFromHome(id) },
             onDelete: { id in requestDeleteFromHome(id) }
         )
     } else if invitationExperienceRolloutEnabled { … existant … } else { … existant … }
     ```
     Garder le texte existant (`invitationExperienceRolloutEnabled`, `EventLibraryView(` via `eventLibraryContent`, `EventListView(`) dans la tranche.
  3. Helpers privés (après `beginRedesignEventCreation`, avant `// MARK: - Home Tab`) :
     - `openEventFromHome(_ id:)` : `guard let event = repository.getEvent(id: id)` ; si `invitationExperienceRolloutEnabled` → même chemin que `eventLibraryContent`'s `onOpenCard`/`navigateToEvent` (lire le code) ; sinon `selectedEvent = event; currentView = .eventDetail` (comme `EventListView.onEventSelected`).
     - `handleHomeNextStep(_:)` : `.vote` → `handleDeepLinkNavigation(.event(.pollVoting(eventId:)))` ; `.pollResults` → `.event(.pollResults(eventId:))` ; `.open` → `openEventFromHome`. (Passer par `handleDeepLinkNavigation` réutilise les gardes d'accès existantes.)
     - `canDeleteFromHome(_:)` : organisateur (`event.organizerId == userId`) et statut non `FINALIZED` (même règle que `EventDetailViewModel.canDelete`).
     - `editDraftFromHome(_:)` : reprendre le chemin « modifier un brouillon » existant (`routeInvitationExperience(InvitationExperienceRouteRequestCanvasAction(action: .editDraft), for:)` après `selectedCreationBaseRevision`/`selectedCreationArtwork`, voir ~l.1323-1334) ; si le flag invitation est éteint, ouvrir le détail.
     - `requestDeleteFromHome(_:)` : `pendingInformationDeleteEvent = event` (réutilise le `confirmationDialog` existant) ; après suppression, incrémenter `eventsHomeReloadToken`.
     - `@State private var eventsHomeReloadToken = 0`, incrémenté aussi quand `currentView` revient à `.eventList` (`.onChange(of: currentView)` dans `redesignChrome`) et quand `redesignRouter.zone` redevient `.events`. `EventsHomeContainer` recharge sur changement de jeton.
  4. `RedesignShellView.header` : ajouter au centre `Text(String(localized: "wk.wordmark")).font(WK.Typo.headline).foregroundStyle(WK.Colors.textTertiary).accessibilityHidden(true)` entre l'avatar et le bouton réglages (`Spacer()` de part et d'autre).

- [ ] **Step 4 :** Lancer `RedesignShellTests`, `EventsHomeViewTests`, `PremiumNavigationContractTests`, `InvitationExperienceArchitectureReviewRedTests`, `InvitationExperienceSurfaceContractTests`, `PremiumDesignSystemContractTests`, `PremiumEventDetailContractTests`, `ParityRouteInventoryContractTests`, `OrganizationPhase5ContractTests`, `OrganizationPhase7ContractTests`, `WakeveAIContractTests`, `WKStyleGuardTests` → verts sans modifier ces tests.

- [ ] **Step 5 : Commit** — `feat(ios): install the new home as the Events zone root` + `Refs Swarm DAO #47 (layer 3).`

### Task 7 : Vérification simulateur et suite complète

- [ ] Build `-derivedDataPath /tmp/wk-l3-dd`, installer sur « iPhone 18 Pro », lancer avec `-iosRedesign2026 YES -hasCompletedOnboarding YES --wakeve-debug-authenticated`. Captures (`xcrun simctl io … screenshot /tmp/wk-l3-*.png`) et **les regarder** : accueil (hero si applicable, grille, statuts, avatars), tap sur une carte → détail puis retour (rechargement), appui long → menu, grande taille de texte (`xcrun simctl ui "iPhone 18 Pro" content_size accessibility-extra-extra-extra-large`) → 1 colonne, mode sombre (`xcrun simctl ui "iPhone 18 Pro" appearance dark`). Remettre taille et apparence par défaut à la fin. Si les 2 événements de démo ne couvrent pas un vote requis, créer un sondage via l'app pour voir la carte « Prochaine étape ».
- [ ] Flag éteint → ancien accueil inchangé.
- [ ] Suite complète `-parallel-testing-enabled NO` → seuls les 5 échecs préexistants.

### Task 8 : Spec et revue

- [ ] Spec §15 : sous-section « Couche 3 » avec les écarts listés en tête de ce plan, + « source de l'accueil : projections de bibliothèque + dépôt (votes) », + « sondages sans créneau daté gardés actifs malgré le classifieur ».
- [ ] Commit `docs(ios): record layer 3 home in redesign spec` + `Refs Swarm DAO #47.`
- [ ] Revue de code (conformité puis qualité).
