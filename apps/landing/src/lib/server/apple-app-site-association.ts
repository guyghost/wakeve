import { env } from '$env/dynamic/private'
import { error, json } from '@sveltejs/kit'

const DEFAULT_IOS_BUNDLE_ID = 'com.guyghost.wakeve'
const APPLE_TEAM_ID_PATTERN = /^[A-Z0-9]{10}$/
const PLACEHOLDER_APPLE_TEAM_IDS = new Set(['ABCDE12345'])

const universalLinkComponents = [
  {
    '/': '/event/*',
    comment: 'Open Wakeve event detail links in the iOS app.'
  },
  {
    '/': '/poll/*',
    comment: 'Open Wakeve poll links in the iOS app.'
  },
  {
    '/': '/meeting/*',
    comment: 'Open Wakeve meeting links in the iOS app.'
  },
  {
    '/': '/invite/*',
    comment: 'Open Wakeve invitation links in the iOS app.'
  }
]

function normalizeAppleTeamId(teamId: string | undefined) {
  const normalizedTeamId = teamId?.trim().toUpperCase()

  if (!normalizedTeamId) return null
  if (!APPLE_TEAM_ID_PATTERN.test(normalizedTeamId)) return null
  if (PLACEHOLDER_APPLE_TEAM_IDS.has(normalizedTeamId)) return null

  return normalizedTeamId
}

export function buildAppleAppSiteAssociation(teamId = env.APPLE_TEAM_ID ?? env.TEAM_ID) {
  const appleTeamId = normalizeAppleTeamId(teamId)

  if (!appleTeamId) return null

  const bundleId = env.IOS_BUNDLE_ID ?? DEFAULT_IOS_BUNDLE_ID
  const appId = `${appleTeamId}.${bundleId}`

  return {
    applinks: {
      apps: [],
      details: [
        {
          appIDs: [appId],
          components: universalLinkComponents
        }
      ]
    }
  }
}

export function appleAppSiteAssociationResponse() {
  const body = buildAppleAppSiteAssociation()

  if (!body) {
    error(503, 'A real APPLE_TEAM_ID or TEAM_ID is required to serve apple-app-site-association')
  }

  return json(body, {
    headers: {
      'cache-control': 'public, max-age=3600'
    }
  })
}
