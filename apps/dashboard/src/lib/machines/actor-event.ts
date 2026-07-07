export function actorOutput<T>(event: unknown): T {
  return (event as { output: T }).output
}

export function actorError(event: unknown): string {
  return String((event as { error: unknown }).error)
}
