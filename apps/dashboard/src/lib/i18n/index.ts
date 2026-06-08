import fr from './fr.json'
import en from './en.json'

// ─── Types ────────────────────────────────────────────────────────────────────

export type Locale = 'fr' | 'en'

type JsonValue = string | number | boolean | null | JsonObject | JsonArray
type JsonObject = { [key: string]: JsonValue }
type JsonArray = JsonValue[]

/** Flat dot-notation key map, e.g. 'nav.events' → string value */
type FlatMap = Record<string, string>

// ─── Storage key ──────────────────────────────────────────────────────────────

const LOCALE_STORAGE_KEY = 'wakeve_locale'
const SUPPORTED_LOCALES: Locale[] = ['fr', 'en']
const DEFAULT_LOCALE: Locale = 'fr'

// ─── Locale catalogues ────────────────────────────────────────────────────────

const catalogues: Record<Locale, JsonObject> = { fr, en }

// ─── Flatten nested JSON into dot-notation keys ───────────────────────────────

function flatten(obj: JsonObject, prefix = ''): FlatMap {
  const result: FlatMap = {}
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (Array.isArray(value)) {
      // Arrays: expose as index-suffixed keys, e.g. 'create.steps.0'
      value.forEach((item, i) => {
        if (typeof item === 'string') {
          result[`${fullKey}.${i}`] = item
        }
      })
    } else if (value !== null && typeof value === 'object') {
      Object.assign(result, flatten(value as JsonObject, fullKey))
    } else {
      result[fullKey] = String(value ?? '')
    }
  }
  return result
}

// Pre-build flat maps for every supported locale
const flatCatalogues: Record<Locale, FlatMap> = {
  fr: flatten(fr),
  en: flatten(en)
}

// ─── Resolve initial locale ───────────────────────────────────────────────────

function resolveLocale(): Locale {
  // 1. Persisted preference
  if (typeof localStorage !== 'undefined') {
    const stored = localStorage.getItem(LOCALE_STORAGE_KEY) as Locale | null
    if (stored && SUPPORTED_LOCALES.includes(stored)) return stored
  }
  // 2. Browser language
  if (typeof navigator !== 'undefined') {
    const lang = navigator.language.split('-')[0] as Locale
    if (SUPPORTED_LOCALES.includes(lang)) return lang
  }
  return DEFAULT_LOCALE
}

// ─── Reactive state (Svelte 5 runes) ─────────────────────────────────────────

let _locale = $state<Locale>(resolveLocale())
let _flatMap = $state<FlatMap>(flatCatalogues[_locale])

// ─── Public API ───────────────────────────────────────────────────────────────

/**
 * Translate a dot-notation key with optional variable interpolation.
 *
 * Variables are written as `{varName}` in the JSON value and replaced by
 * the matching property in the `vars` object.
 *
 * @example
 *   t('auth.verifySubtitle', { email: 'a@b.com' })
 *   // → "Enter the code sent to a@b.com"
 *
 * @example
 *   t('create.slots.slot', { n: '2' })
 *   // → "Slot 2"
 */
export function t(key: string, vars?: Record<string, string | number>): string {
  let value = _flatMap[key]

  if (value === undefined) {
    // Fallback: try the default locale before giving up
    value = flatCatalogues[DEFAULT_LOCALE][key]
  }

  if (value === undefined) {
    // Last resort: return the key itself so missing strings are obvious
    return key
  }

  if (vars) {
    for (const [name, replacement] of Object.entries(vars)) {
      value = value.replaceAll(`{${name}}`, String(replacement))
    }
  }

  return value
}

/**
 * Switch the active locale, persist to localStorage and update reactive state.
 */
export function setLocale(locale: Locale): void {
  if (!SUPPORTED_LOCALES.includes(locale)) return
  _locale = locale
  _flatMap = flatCatalogues[locale]
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  }
}

/**
 * Read the current locale (reactive — use inside Svelte templates / $derived).
 */
export function getLocale(): Locale {
  return _locale
}

/**
 * Reactive getter — equivalent to `getLocale()` but exported as a plain
 * property for convenience in Svelte templates.
 *
 * @example
 *   <p>{currentLocale}</p>
 */
export const currentLocale = {
  get value(): Locale {
    return _locale
  }
}

/**
 * Check whether a locale is supported.
 */
export function isSupportedLocale(lang: string): lang is Locale {
  return SUPPORTED_LOCALES.includes(lang as Locale)
}

/** All locales the app supports. */
export { SUPPORTED_LOCALES }
