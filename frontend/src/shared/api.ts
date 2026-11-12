export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly correlationId: string | null,
  ) {
    super(`Le serveur a répondu avec le statut ${status}.`)
  }
}

export async function getJson(path: `/api/${string}`, signal?: AbortSignal): Promise<unknown> {
  const response = await fetch(path, {
    signal,
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new ApiError(response.status, response.headers.get('X-Correlation-Id'))
  }
  return response.json()
}
