import { NextRequest, NextResponse } from 'next/server'

/**
 * GET /api/integrations/google/status
 * 
 * Check if Google Calendar is connected for the user
 * 
 * Response:
 * {
 *   "service": "GOOGLE_CALENDAR",
 *   "connected": boolean,
 *   "email"?: string,
 *   "connectedAt"?: ISO-8601-date
 * }
 */
export async function GET(request: NextRequest) {
  try {
    // Check for coagent_session cookie
    const sessionCookie = request.cookies.get('coagent_session')

    if (!sessionCookie) {
      return NextResponse.json(
        { error: 'Unauthorized' },
        { status: 401 }
      )
    }

    // In production: query database for user's Google Calendar integration
    // For now, return mock data
    return NextResponse.json({
      service: 'GOOGLE_CALENDAR',
      connected: true,
      email: 'alex@gmail.com',
      connectedAt: new Date().toISOString(),
    })
  } catch (error) {
    console.error('[API] Google status check failed:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}
