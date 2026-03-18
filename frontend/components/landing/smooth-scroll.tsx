"use client"

import { ReactLenis } from 'lenis/react'
import { ReactNode, useEffect } from 'react'

export function SmoothScroll({ children }: { children: ReactNode }) {
  useEffect(() => {
    // Basic setup to ensure it mounts properly
  }, [])

  return (
    <ReactLenis root options={{
      lerp: 0.1,
      duration: 1.2,
      smoothWheel: true,
      wheelMultiplier: 0.60 // Reduces the speed of scroll steps by exactly 40% (additional 15% reduction)
    }}>
      {children}
    </ReactLenis>
  )
}
