/**
 * Public i18n entry point.
 *
 * The reactive implementation lives in `index.svelte.ts`: Svelte 5 runes
 * (`$state`) are only compiled in `.svelte.ts` modules, so keeping the
 * catalogue there is what makes `t()` / `getLocale()` reactive.
 * This file re-exports the API so the `$lib/i18n` specifier keeps resolving.
 */
export {
  t,
  setLocale,
  getLocale,
  currentLocale,
  isSupportedLocale,
  SUPPORTED_LOCALES
} from './index.svelte.js'

export type { Locale } from './index.svelte.js'
