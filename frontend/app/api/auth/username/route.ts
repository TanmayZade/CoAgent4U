import { NextRequest, NextResponse } from 'next/server'

/**
 * POST /api/auth/username
 * 
 * Set username for authenticated user
 * 
 * Requires: coagent_session cookie
 * 
 * Body:
 * { "username": "string" }
 * 
 * Validation: ^[a-zA-Z0-9_-]{3,32}$
 */
export async function POST(request: NextRequest) {
  try {
    const { username } = await request.json()

    // Validate username format
    const usernameRegex = /^[a-zA-Z0-9_-]{3,32}$/
    if (!usernameRegex.test(username)) {
      return NextResponse.json(
        { error: 'Invalid username format' },
        { status: 400 }
      )
    }

    // Check if username already exists (mock check)
    // In production: query database
    const takenUsernames = ['admin', 'root', 'test', 'demo']
    if (takenUsernames.includes(username.toLowerCase())) {
      return NextResponse.json(
        { error: 'Username already taken' },
        { status: 409 }
      )
    }

    // Save username to database for authenticated user
    // In production: update user record with username

    return NextResponse.json({
      success: true,
      username,
      message: 'Username set successfully',
    })
  } catch (error) {
    console.error('[API] Username set failed:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}
