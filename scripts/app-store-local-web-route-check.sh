#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# App Store local web route check — Wakeve
# Validates public legal/support pages and AASA endpoints on a
# local or preview web deployment before production URL checks.
# ──────────────────────────────────────────────────────────────

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:4174}"
APPLE_TEAM_ID="${APPLE_TEAM_ID:-A1B2C3D4E5}"
IOS_BUNDLE_ID="${IOS_BUNDLE_ID:-com.guyghost.wakeve}"

node - "$BASE_URL" "$APPLE_TEAM_ID" "$IOS_BUNDLE_ID" <<'NODE'
const [baseUrl, appleTeamId, iosBundleId] = process.argv.slice(2)

const pages = [
  ['/privacy', ['Privacy Policy', 'Information We Collect', 'privacy@wakeve.app']],
  ['/support', ['Support', 'support@wakeve.app', 'Third-Party Notices']],
  ['/terms', ['Terms of Service', 'User Conduct and Content', 'legal@wakeve.app']],
  ['/third-party-notices', ['Third-Party Notices', 'Dependency Notices', 'Review Status']]
]

const aasaPaths = [
  '/.well-known/apple-app-site-association',
  '/apple-app-site-association'
]

const expectedAasaPaths = ['/event/*', '/poll/*', '/meeting/*', '/invite/*']
const expectedAppID = `${appleTeamId}.${iosBundleId}`
let failures = 0

async function checkPage(path, phrases) {
  const response = await fetch(new URL(path, baseUrl))
  const body = await response.text()
  const missing = phrases.filter((phrase) => !body.includes(phrase))
  const ok = response.status === 200 && missing.length === 0

  console.log(`${ok ? 'PASS' : 'FAIL'} ${path} status=${response.status} content-type=${response.headers.get('content-type') ?? ''}`)
  if (missing.length > 0) console.log(`  missing=${missing.join(', ')}`)
  if (!ok) failures += 1
}

async function checkAasa(path) {
  const response = await fetch(new URL(path, baseUrl))
  const contentType = response.headers.get('content-type') ?? ''
  let body

  try {
    body = await response.json()
  } catch {
    console.log(`FAIL ${path} invalid-json status=${response.status} content-type=${contentType}`)
    failures += 1
    return
  }

  const details = body?.applinks?.details ?? []
  const appIDs = details.flatMap((detail) => detail.appIDs ?? [])
  const componentPaths = details
    .flatMap((detail) => detail.components ?? [])
    .map((component) => component['/'])

  const ok =
    response.status === 200 &&
    contentType.includes('application/json') &&
    appIDs.includes(expectedAppID) &&
    expectedAasaPaths.every((expectedPath) => componentPaths.includes(expectedPath))

  console.log(`${ok ? 'PASS' : 'FAIL'} ${path} status=${response.status} content-type=${contentType} appIDs=${appIDs.join(',')} paths=${componentPaths.join(',')}`)
  if (!ok) failures += 1
}

for (const [path, phrases] of pages) {
  await checkPage(path, phrases)
}

for (const path of aasaPaths) {
  await checkAasa(path)
}

process.exitCode = failures === 0 ? 0 : 1
NODE
