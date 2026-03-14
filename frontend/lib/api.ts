/**
 * API utility for making requests to the backend server
 * Uses NEXT_PUBLIC_API_URL environment variable for the base URL
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'https://api.coagent4u.com'

export class APIError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message)
    this.name = 'APIError'
  }
}

interface RequestOptions extends RequestInit {
  includeCredentials?: boolean
}

/**
 * Make an API request to the backend server
 * @param endpoint - The endpoint path (e.g., '/auth/session')
 * @param options - Fetch options (method, body, headers, etc.)
 * @returns The parsed JSON response
 */
export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { includeCredentials = true, ...fetchOptions } = options

  const url = `${API_URL}${endpoint}`
  
  const response = await fetch(url, {
    ...fetchOptions,
    credentials: includeCredentials ? 'include' : 'omit',
    headers: {
      'Content-Type': 'application/json',
      ...fetchOptions.headers,
    },
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    const errorMessage = (errorData as any).message || `Request failed with status ${response.status}`
    throw new APIError(response.status, errorMessage)
  }

  return response.json()
}

/**
 * Build API endpoint URL (useful for direct redirects)
 * @param endpoint - The endpoint path
 * @returns The full API URL
 */
export function getAPIUrl(endpoint: string): string {
  return `${API_URL}${endpoint}`
}

// Auth API endpoints
export const authAPI = {
  checkSession: () => apiRequest('/auth/session'),
  getMe: () => apiRequest('/auth/me'),
  setUsername: (username: string) =>
    apiRequest('/auth/username', {
      method: 'POST',
      body: JSON.stringify({ username }),
    }),
  logout: () => apiRequest('/auth/logout', { method: 'POST' }),
  slackStart: () => getAPIUrl('/auth/slack/start'),
  slackInstallStart: () => getAPIUrl('/auth/slack/install/start'),
}

// Integration API endpoints
export const integrationAPI = {
  googleAuthorize: () => getAPIUrl('/integrations/google/authorize'),
}
