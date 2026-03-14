import { NextRequest, NextResponse } from 'next/server'

/**
 * GET /api/auth/session
 * 
 * Returns the current session data based on HTTP-only cookie
 * 
 * Response:
 * {
 *   authenticated: boolean,
 *   username?: string,
 *   email?: string,
 *   pendingRegistration: boolean,
 *   googleCalendarConnected?: boolean
 * }
 */
export async function GET(request: NextRequest) {
  try {
    // Check for coagent_session cookie
    const sessionCookie = request.cookies.get('coagent_session')

    if (!sessionCookie) {
      return NextResponse.json(
        {
          authenticated: false,
          pendingRegistration: false,
        },
        { status: 401 }
      )
    }

    // In production, verify the session token and fetch user data from database
    // For now, return a mock authenticated session
    return NextResponse.json({
      authenticated: true,
      username: 'alex-johnson',
      email: 'alex@example.com',
      pendingRegistration: false,
      googleCalendarConnected: true,
    })
  } catch (error) {
    console.error('[API] Session check failed:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}
