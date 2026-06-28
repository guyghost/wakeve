#!/usr/bin/env node
import { mkdirSync, readdirSync, readFileSync, writeFileSync, statSync } from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'

const repoRoot = path.resolve(new URL('..', import.meta.url).pathname)
const docsDir = path.join(repoRoot, 'docs', 'qa')
const localQaDir = path.join(repoRoot, '.local', 'qa')
const inventoryPath = path.join(docsDir, 'local-production-like-qa-inventory.md')
const fixturePath = path.join(localQaDir, 'wakeve-production-like-fixtures.json')

function walk(dir, predicate = () => true) {
  const out = []
  if (!exists(dir)) return out
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry)
    const stat = statSync(full)
    if (stat.isDirectory()) out.push(...walk(full, predicate))
    else if (predicate(full)) out.push(full)
  }
  return out.sort()
}

function exists(file) {
  try {
    statSync(file)
    return true
  } catch {
    return false
  }
}

function rel(file) {
  return path.relative(repoRoot, file)
}

function routeFromSvelteKitFile(file) {
  const srcRoutes = file.includes('/apps/dashboard/')
    ? path.join(repoRoot, 'apps', 'dashboard', 'src', 'routes')
    : path.join(repoRoot, 'apps', 'landing', 'src', 'routes')
  const app = file.includes('/apps/dashboard/') ? 'dashboard' : 'landing'
  const parts = path
    .relative(srcRoutes, file)
    .split(path.sep)
    .filter((part) => !part.startsWith('('))
    .filter((part) => !part.startsWith('+'))
    .map((part) => part.replace(/^\[\.\.\.(.+)]$/, '*$1').replace(/^\[(.+)]$/, ':$1'))
  return {
    app,
    route: `/${parts.join('/')}`.replace(/\/$/, '') || '/',
    kind: file.endsWith('+page.svelte') || file.endsWith('+layout.svelte') ? 'page' : 'endpoint',
    file: rel(file)
  }
}

function lineNumberAt(text, index) {
  return text.slice(0, index).split('\n').length
}

function clean(value) {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .replace(/[|]/g, '\\|')
    .trim()
}

function extractAttribute(fragment, name) {
  const match = fragment.match(new RegExp(`${name}=\\{?"([^"}]+)"\\}?`))
  return match ? clean(match[1]) : ''
}

function extractInteractions(file) {
  const text = readFileSync(file, 'utf8')
  const patterns = [
    ['button', /<button\b[\s\S]*?>/g],
    ['button-component', /<Button\b[\s\S]*?>/g],
    ['link', /<a\b[\s\S]*?>/g],
    ['input', /<input\b[\s\S]*?>/g],
    ['textarea', /<textarea\b[\s\S]*?>|<Textarea\b[\s\S]*?>/g],
    ['select', /<select\b[\s\S]*?>|<Select\b[\s\S]*?>/g],
    ['modal', /<dialog\b[\s\S]*?>|<Modal\b[\s\S]*?>/g],
    ['status-region', /role="(?:alert|status|switch|progressbar|tooltip|group|list)"/g]
  ]
  const items = []
  for (const [kind, pattern] of patterns) {
    for (const match of text.matchAll(pattern)) {
      const fragment = match[0]
      items.push({
        kind,
        line: lineNumberAt(text, match.index ?? 0),
        label:
          extractAttribute(fragment, 'aria-label') ||
          extractAttribute(fragment, 'placeholder') ||
          extractAttribute(fragment, 'href') ||
          extractAttribute(fragment, 'type') ||
          clean(fragment.slice(0, 90)),
        risk: riskForInteraction(kind, fragment)
      })
    }
  }
  return items.sort((a, b) => a.line - b.line)
}

function riskForInteraction(kind, fragment) {
  if (kind === 'modal' || fragment.includes('delete') || fragment.includes('SUPPRIMER')) return 'destructive-confirmation'
  if (fragment.includes('email') || fragment.includes('otp') || fragment.includes('password')) return 'auth-and-input-validation'
  if (fragment.includes('switch') || fragment.includes('aria-checked')) return 'preference-state'
  if (kind === 'input' || kind === 'textarea' || kind === 'select') return 'input-validation'
  if (fragment.includes('href=') || kind === 'link') return 'navigation'
  return 'interaction-state'
}

function extractMachines() {
  const files = walk(path.join(repoRoot, 'apps', 'dashboard', 'src', 'lib', 'machines'), (file) => file.endsWith('.ts'))
  return files.map((file) => {
    const text = readFileSync(file, 'utf8')
    const stateNames = new Set()
    for (const match of text.matchAll(/initial:\s*'([^']+)'/g)) stateNames.add(match[1])
    for (const match of text.matchAll(/target:\s*'([^']+)'/g)) stateNames.add(match[1])
    for (const match of text.matchAll(/^\s{4}([A-Za-z0-9_]+):\s*\{/gm)) {
      const name = match[1]
      if (!['types', 'actors', 'actions', 'guards', 'context', 'events', 'input'].includes(name)) stateNames.add(name)
    }
    const events = [...text.matchAll(/\|\s*\{\s*type:\s*'([^']+)'/g)].map((match) => match[1])
    return {
      id: path.basename(file, '.machine.ts'),
      file: rel(file),
      states: [...stateNames].sort(),
      events: [...new Set(events)].sort()
    }
  })
}

function extractServerRoutes() {
  const files = walk(path.join(repoRoot, 'server', 'src', 'main', 'kotlin'), (file) => file.endsWith('.kt'))
  const routes = []
  for (const file of files) {
    const text = readFileSync(file, 'utf8')
    const lines = text.split('\n')
    lines.forEach((line, idx) => {
      const routeMatch = line.match(/\b(route|get|post|put|patch|delete)\("([^"]*)"\)/)
      if (!routeMatch) return
      routes.push({
        method: routeMatch[1].toUpperCase(),
        path: routeMatch[2] || '/',
        file: `${rel(file)}:${idx + 1}`,
        risk: routeRisk(routeMatch[1], routeMatch[2], text)
      })
    })
  }
  return routes.sort((a, b) => `${a.file}${a.method}${a.path}`.localeCompare(`${b.file}${b.method}${b.path}`))
}

function routeRisk(method, routePath, fileText) {
  const route = `${method} ${routePath}`.toLowerCase()
  if (method === 'delete' || route.includes('delete')) return 'destructive'
  if (route.includes('auth') || route.includes('session') || fileText.includes('authenticate')) return 'auth-access'
  if (route.includes('payment') || route.includes('tricount') || route.includes('budget')) return 'financial'
  if (route.includes('sync') || route.includes('offline')) return 'sync-conflict'
  if (route.includes('meeting') || route.includes('calendar') || route.includes('notification')) return 'external-integration'
  return 'business-workflow'
}

function extractSpecs() {
  const specFiles = walk(path.join(repoRoot, 'openspec', 'specs'), (file) => file.endsWith('spec.md'))
  return specFiles.map((file) => {
    const text = readFileSync(file, 'utf8')
    return {
      capability: path.basename(path.dirname(file)),
      file: rel(file),
      requirements: [...text.matchAll(/^### Requirement:\s*(.+)$/gm)].map((match) => clean(match[1])),
      scenarios: [...text.matchAll(/^#### Scenario:\s*(.+)$/gm)].map((match) => clean(match[1]))
    }
  }).sort((a, b) => a.capability.localeCompare(b.capability))
}

function acceptanceForRoute(route, interactions) {
  const criteria = [
    'The route loads without console/runtime errors for the authorized role.',
    'Loading, empty, success, error, and offline/slow-network states remain readable on mobile.',
    'Navigation and primary actions preserve the expected workflow state.',
    'No secret, token, real personal data, or production identifier is rendered or logged.'
  ]
  if (interactions.some((item) => item.kind.includes('input') || item.kind === 'textarea' || item.kind === 'select')) {
    criteria.push('Each input validates required, invalid, duplicate, long, and whitespace-only values.')
  }
  if (interactions.some((item) => item.kind === 'modal' || item.risk === 'destructive-confirmation')) {
    criteria.push('Destructive actions require explicit confirmation and remain cancelable.')
  }
  if (route.route.includes(':') || route.route.includes('*')) {
    criteria.push('Unknown, unauthorized, and malformed identifiers return controlled error states.')
  }
  return criteria
}

function edgeCasesForRisk(risk) {
  const map = {
    'auth-and-input-validation': ['invalid email', 'empty OTP', 'expired session', 'network timeout', 'duplicate submission'],
    'input-validation': ['empty required field', 'whitespace-only value', 'max-length value', 'invalid date/time order', 'duplicate entry'],
    'destructive-confirmation': ['cancel action', 'wrong confirmation text', 'double submit', 'expired session', 'server failure after optimistic UI'],
    'preference-state': ['toggle twice', 'reload persistence', 'offline update', 'keyboard activation'],
    navigation: ['back/forward navigation', 'unknown route parameter', 'mobile menu close', 'focus restoration'],
    'interaction-state': ['disabled loading state', 'rapid repeated click', 'keyboard activation', 'offline retry'],
    'auth-access': ['missing token', 'expired token', 'wrong role', 'guest user', 'non-member access'],
    destructive: ['not owner', 'already deleted', 'dependent records', 'audit log redaction'],
    financial: ['negative amount', 'currency rounding', 'untrusted external URL', 'participant removed'],
    'sync-conflict': ['duplicate operation id', 'clock skew', 'pending conflict', 'retry after failure'],
    'external-integration': ['provider unavailable', 'invalid callback payload', 'missing permission', 'safe fallback'],
    'business-workflow': ['invalid status transition', 'empty result', 'pagination boundary', 'timezone boundary']
  }
  return map[risk] ?? map['business-workflow']
}

function generateFixtures({ users = 600, events = 180 } = {}) {
  const rng = mulberry32(0x57414b45)
  const roles = ['ORGANIZER', 'PARTICIPANT']
  const statuses = ['DRAFT', 'POLLING', 'CONFIRMED', 'COMPARING', 'ORGANIZING', 'FINALIZED']
  const eventTypes = ['TRIP', 'DINNER', 'BIRTHDAY', 'WEDDING', 'WORKSHOP', 'OTHER']
  const userRows = Array.from({ length: users }, (_, i) => {
    const id = `synthetic-user-${String(i + 1).padStart(4, '0')}`
    return {
      id,
      displayName: `Synthetic User ${i + 1}`,
      email: `synthetic.user.${i + 1}@example.test`,
      accountType: i % 11 === 0 ? 'GUEST' : 'REGISTERED',
      authMethod: i % 11 === 0 ? 'GUEST' : 'EMAIL_OTP',
      consentVersion: 'local-qa-v1'
    }
  })
  const eventsRows = []
  const participantRows = []
  const slotRows = []
  const voteRows = []
  const commentRows = []
  const transportRows = []
  for (let i = 0; i < events; i++) {
    const organizer = userRows[i % userRows.length]
    const eventId = `synthetic-event-${String(i + 1).padStart(4, '0')}`
    const participantCount = 8 + Math.floor(rng() * 35)
    const status = statuses[i % statuses.length]
    const deadline = new Date(Date.UTC(2026, 6 + (i % 4), 5 + (i % 21), 18, 0, 0)).toISOString()
    eventsRows.push({
      id: eventId,
      title: `Synthetic planning event ${i + 1}`,
      description: `Local QA fixture covering ${status.toLowerCase()} workflow with synthetic participants.`,
      organizerId: organizer.id,
      type: eventTypes[i % eventTypes.length],
      status,
      expectedParticipants: participantCount,
      timezone: i % 3 === 0 ? 'Europe/Paris' : i % 3 === 1 ? 'America/New_York' : 'Asia/Tokyo',
      deadline
    })
    const selectedUsers = pickUsers(userRows, i, participantCount)
    selectedUsers.forEach((user, index) => {
      const participantId = `${eventId}-participant-${String(index + 1).padStart(3, '0')}`
      const confirmed = ['CONFIRMED', 'ORGANIZING', 'FINALIZED'].includes(status) && index % 4 !== 0
      participantRows.push({
        id: participantId,
        eventId,
        userId: user.id,
        role: user.id === organizer.id ? 'ORGANIZER' : roles[1],
        status: confirmed ? 'CONFIRMED' : index % 5 === 0 ? 'DECLINED' : 'INVITED'
      })
      if (confirmed) {
        transportRows.push({
          eventId,
          userId: user.id,
          locationName: `Synthetic station ${((i + index) % 24) + 1}`,
          latitude: round(40 + rng() * 12),
          longitude: round(-5 + rng() * 15),
          source: 'LOCAL_QA_SYNTHETIC'
        })
      }
    })
    for (let slot = 0; slot < 4; slot++) {
      const start = new Date(Date.UTC(2026, 7 + (i % 4), 10 + slot, 8 + slot * 2, 0, 0))
      const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
      const slotId = `${eventId}-slot-${slot + 1}`
      slotRows.push({
        id: slotId,
        eventId,
        start: start.toISOString(),
        end: end.toISOString(),
        timezone: eventsRows[i].timezone,
        timeOfDay: ['MORNING', 'AFTERNOON', 'EVENING', 'ALL_DAY'][slot]
      })
      selectedUsers.slice(0, Math.min(selectedUsers.length, 24)).forEach((user, voterIndex) => {
        voteRows.push({
          eventId,
          slotId,
          userId: user.id,
          value: ['YES', 'MAYBE', 'NO'][(voterIndex + slot + i) % 3]
        })
      })
    }
    for (let c = 0; c < Math.min(6, Math.ceil(participantCount / 7)); c++) {
      commentRows.push({
        id: `${eventId}-comment-${c + 1}`,
        eventId,
        authorId: selectedUsers[c % selectedUsers.length].id,
        section: ['GENERAL', 'POLL', 'LOGISTICS'][c % 3],
        content: `Synthetic local QA comment ${c + 1}; no personal data.`
      })
    }
  }
  return {
    manifest: {
      generatedAt: new Date().toISOString(),
      purpose: 'Local production-like QA only',
      safety: [
        'Synthetic identities only',
        'Reserved example.test email domain',
        'No access tokens, passwords, OAuth secrets, or production identifiers',
        'Written under .local/qa, which is gitignored'
      ],
      scale: {
        users: userRows.length,
        events: eventsRows.length,
        participants: participantRows.length,
        slots: slotRows.length,
        votes: voteRows.length,
        comments: commentRows.length,
        transportDepartures: transportRows.length
      }
    },
    users: userRows,
    events: eventsRows,
    participants: participantRows,
    slots: slotRows,
    votes: voteRows,
    comments: commentRows,
    transportDepartures: transportRows
  }
}

function pickUsers(users, offset, count) {
  const selected = []
  for (let i = 0; i < count; i++) selected.push(users[(offset * 13 + i * 7) % users.length])
  return selected
}

function round(n) {
  return Math.round(n * 100000) / 100000
}

function mulberry32(seed) {
  return function next() {
    let t = seed += 0x6D2B79F5
    t = Math.imul(t ^ t >>> 15, t | 1)
    t ^= t + Math.imul(t ^ t >>> 7, t | 61)
    return ((t ^ t >>> 14) >>> 0) / 4294967296
  }
}

function sha256(file) {
  return crypto.createHash('sha256').update(readFileSync(file)).digest('hex')
}

function markdown() {
  const routeFiles = walk(path.join(repoRoot, 'apps'), (file) =>
    file.endsWith('+page.svelte') || file.endsWith('+layout.svelte') || file.endsWith('+server.ts')
  ).filter((file) => file.includes('/apps/dashboard/') || file.includes('/apps/landing/'))
  const pageRoutes = routeFiles.map(routeFromSvelteKitFile)
  const interactionsByFile = new Map()
  for (const route of pageRoutes.filter((item) => item.kind === 'page')) {
    const full = path.join(repoRoot, route.file)
    interactionsByFile.set(route.file, extractInteractions(full))
  }
  const machines = extractMachines()
  const serverRoutes = extractServerRoutes()
  const specs = extractSpecs()
  const fixture = generateFixtures()

  mkdirSync(docsDir, { recursive: true })
  mkdirSync(localQaDir, { recursive: true })
  writeFileSync(fixturePath, `${JSON.stringify(fixture, null, 2)}\n`)

  const lines = []
  lines.push('# Local Production-Like QA Inventory')
  lines.push('')
  lines.push(`Generated: ${new Date().toISOString()}`)
  lines.push('')
  lines.push('## Safety Boundary')
  lines.push('')
  lines.push('- Scope: local synthetic QA only.')
  lines.push('- Production access: not used.')
  lines.push('- Sensitive data: not used; emails use the reserved `example.test` domain.')
  lines.push('- Destructive actions: not executed against production or shared developer data.')
  lines.push(`- Fixture output: \`.local/qa/wakeve-production-like-fixtures.json\` (${fixture.manifest.scale.users} users, ${fixture.manifest.scale.events} events, ${fixture.manifest.scale.votes} votes).`)
  lines.push(`- Fixture SHA-256: \`${sha256(fixturePath)}\`.`)
  lines.push('')
  lines.push('## Roles')
  lines.push('')
  lines.push('| Role | User-facing responsibility | Acceptance criteria | Risk-focused edge cases |')
  lines.push('|---|---|---|---|')
  lines.push('| Guest | Try Wakeve with limited authenticated state. | Guest can enter the app without real credentials and cannot access restricted data. | expired guest session; denied protected route; logout cleanup |')
  lines.push('| Registered participant | Vote, comment, inspect event details, and update preferences. | Participant sees only permitted event data and receives clear success/error/offline states. | non-member access; duplicate vote; removed participant; slow network |')
  lines.push('| Organizer | Create events, manage workflow decisions, and inspect dashboard actions. | Organizer-only actions require correct role and preserve workflow status. | invalid transition; missing slots; duplicate invites; destructive cancellation |')
  lines.push('| Non-member | External or unauthorized visitor. | Non-member receives controlled auth/permission states without data disclosure. | malformed id; missing token; stale link |')
  lines.push('')
  lines.push('## Web Routes And User Surface')
  lines.push('')
  lines.push('| App | Route | Kind | File | User-facing controls | Acceptance criteria | Limited edge cases |')
  lines.push('|---|---|---|---|---:|---|---|')
  for (const route of pageRoutes) {
    const interactions = interactionsByFile.get(route.file) ?? []
    const criteria = route.kind === 'page'
      ? acceptanceForRoute(route, interactions)
      : ['Endpoint returns expected redirect/response without leaking internal paths or secrets.']
    const risks = new Set(interactions.map((item) => item.risk))
    const edges = [...risks].flatMap(edgeCasesForRisk).slice(0, 8)
    lines.push(`| ${route.app} | \`${route.route}\` | ${route.kind} | \`${route.file}\` | ${interactions.length} | ${criteria.map((item) => `- ${item}`).join('<br>')} | ${edges.join('; ') || 'route method/status boundaries'} |`)
  }
  lines.push('')
  lines.push('## Buttons, Inputs, Modals, And Status Regions')
  lines.push('')
  lines.push('| File | Line | Type | Label/source | Risk | Acceptance criteria | Edge cases |')
  lines.push('|---|---:|---|---|---|---|---|')
  for (const [file, interactions] of interactionsByFile) {
    for (const item of interactions) {
      lines.push(`| \`${file}\` | ${item.line} | ${item.kind} | ${clean(item.label)} | ${item.risk} | Visible label/role, keyboard access, loading/disabled state, deterministic result. | ${edgeCasesForRisk(item.risk).join('; ')} |`)
    }
  }
  lines.push('')
  lines.push('## State Machines And User Workflows')
  lines.push('')
  lines.push('| Machine | File | States | User events | Acceptance criteria | Edge cases |')
  lines.push('|---|---|---|---|---|---|')
  for (const machine of machines) {
    lines.push(`| ${machine.id} | \`${machine.file}\` | ${machine.states.map((state) => `\`${state}\``).join(', ')} | ${machine.events.map((event) => `\`${event}\``).join(', ')} | Every transition has an observable loading/success/error result and preserves context needed for retry. | stale context; retry from error; rapid duplicate event; network offline; expired auth |`)
  }
  lines.push('')
  lines.push('## Backend Routes')
  lines.push('')
  lines.push('| Method | Path snippet | Source | Risk | Acceptance criteria | Edge cases |')
  lines.push('|---|---|---|---|---|---|')
  for (const route of serverRoutes) {
    lines.push(`| ${route.method} | \`${clean(route.path)}\` | \`${route.file}\` | ${route.risk} | Valid request returns documented status/body; invalid/authz failures return localized safe errors; no secret or internal stack is exposed. | ${edgeCasesForRisk(route.risk).join('; ')} |`)
  }
  lines.push('')
  lines.push('## OpenSpec Capabilities')
  lines.push('')
  lines.push('| Capability | Requirements | Scenario count | Source | Acceptance criteria |')
  lines.push('|---|---:|---:|---|---|')
  for (const spec of specs) {
    lines.push(`| ${spec.capability} | ${spec.requirements.length} | ${spec.scenarios.length} | \`${spec.file}\` | All listed scenarios remain passable with local synthetic data and role-appropriate access. |`)
  }
  lines.push('')
  lines.push('## Test Log')
  lines.push('')
  lines.push('| Time | Command/flow | Result | Evidence | Bugs logged |')
  lines.push('|---|---|---|---|---|')
  lines.push('| pending | `pnpm --filter wakeve-dashboard check` | pending | to run after inventory generation | none yet |')
  lines.push('')
  lines.push('## Bug Log')
  lines.push('')
  lines.push('| ID | Surface | Severity | Reproduction proof | Suspected cause/dependency | Regression test | Status |')
  lines.push('|---|---|---|---|---|---|---|')
  lines.push('| none-yet | n/a | n/a | No failing user test has been run in this cycle yet. | n/a | n/a | open |')
  lines.push('')
  lines.push('## Common-Cause Analysis Checklist')
  lines.push('')
  lines.push('- Auth/session failures usually affect login, protected layouts, dashboard, profile, settings, and event detail together.')
  lines.push('- API schema drift affects dashboard machines, event list/detail machines, and create wizard submission together.')
  lines.push('- Offline or retry gaps affect dashboard reload, event list reload, vote/comment submission, and sync routes together.')
  lines.push('- Destructive confirmation gaps are concentrated in account deletion and comment deletion surfaces.')
  lines.push('- Date/time validation issues affect create wizard slots, poll voting, calendar, weather, and meeting flows.')
  lines.push('')
  writeFileSync(inventoryPath, `${lines.join('\n')}\n`)
  return { inventoryPath, fixturePath, fixture, routeCount: pageRoutes.length, interactionCount: [...interactionsByFile.values()].reduce((sum, items) => sum + items.length, 0), serverRouteCount: serverRoutes.length }
}

const result = markdown()
console.log(`inventory=${rel(result.inventoryPath)}`)
console.log(`fixture=${rel(result.fixturePath)}`)
console.log(`users=${result.fixture.manifest.scale.users}`)
console.log(`events=${result.fixture.manifest.scale.events}`)
console.log(`interactions=${result.interactionCount}`)
console.log(`web_routes=${result.routeCount}`)
console.log(`server_routes=${result.serverRouteCount}`)
