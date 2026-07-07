import { redirect, type RequestEvent } from '@sveltejs/kit'

export function GET({ params }: RequestEvent) {
  redirect(308, `/app/events/${params.path}`)
}
