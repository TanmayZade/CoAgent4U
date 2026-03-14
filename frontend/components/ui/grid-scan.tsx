'use client'

import { useEffect, useState } from 'react'

interface GridScanProps {
  lineThickness?: number
  linesColor?: string
  scanColor?: string
  scanOpacity?: number
  gridScale?: number
  lineStyle?: 'solid' | 'dashed'
  scanDirection?: 'horizontal' | 'vertical' | 'pingpong'
  scanGlow?: number
  scanSoftness?: number
  scanDuration?: number
  scanDelay?: number
  scanOnClick?: boolean
  className?: string
}

export function GridScan({
  lineThickness = 1,
  linesColor = '#392e4e',
  scanColor = '#ffffff',
  scanOpacity = 0.4,
  scanGlow = 0.5,
  scanSoftness = 2,
  scanDuration = 2,
  scanDelay = 2,
  className,
}: GridScanProps) {
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) {
    return <div className={className} />
  }

  // Create CSS variables for the animation
  const scanKeyframes = `
    @keyframes scanMove {
      0%, 100% { transform: translateY(-100%); }
      50% { transform: translateY(100vh); }
    }
  `

  return (
    <div className={className} style={{ overflow: 'hidden' }}>
      <style>{scanKeyframes}</style>
      
      {/* 3D Perspective Grid Container */}
      <div 
        style={{
          position: 'absolute',
          inset: 0,
          perspective: '500px',
          perspectiveOrigin: '50% 50%',
          overflow: 'hidden',
        }}
      >
        {/* Floor Grid */}
        <div
          style={{
            position: 'absolute',
            left: '-50%',
            right: '-50%',
            top: '50%',
            height: '100%',
            transformOrigin: 'center top',
            transform: 'rotateX(75deg)',
            backgroundImage: `
              linear-gradient(${linesColor} ${lineThickness}px, transparent ${lineThickness}px),
              linear-gradient(90deg, ${linesColor} ${lineThickness}px, transparent ${lineThickness}px)
            `,
            backgroundSize: '60px 60px',
            opacity: 0.6,
          }}
        />

        {/* Ceiling Grid */}
        <div
          style={{
            position: 'absolute',
            left: '-50%',
            right: '-50%',
            bottom: '50%',
            height: '100%',
            transformOrigin: 'center bottom',
            transform: 'rotateX(-75deg)',
            backgroundImage: `
              linear-gradient(${linesColor} ${lineThickness}px, transparent ${lineThickness}px),
              linear-gradient(90deg, ${linesColor} ${lineThickness}px, transparent ${lineThickness}px)
            `,
            backgroundSize: '60px 60px',
            opacity: 0.6,
          }}
        />

        {/* Left Wall Grid */}
        <div
          style={{
            position: 'absolute',
            top: '-50%',
            bottom: '-50%',
            right: '50%',
            width: '100%',
            transformOrigin: 'right center',
            transform: 'rotateY(-75deg)',
            backgroundImage: `
              linear-gradient(${linesColor} ${lineThickness}px, transparent ${lineThickness}px),
              linear-gradient(90deg, ${linesColor} ${lineThickness}px, transparent ${lineThickness}px)
            `,
            backgroundSize: '60px 60px',
            opacity: 0.5,
          }}
        />

        {/* Right Wall Grid */}
        <div
          style={{
            position: 'absolute',
            top: '-50%',
            bottom: '-50%',
            left: '50%',
            width: '100%',
            transformOrigin: 'left center',
            transform: 'rotateY(75deg)',
            backgroundImage: `
              linear-gradient(${linesColor} ${lineThickness}px, transparent ${lineThickness}px),
              linear-gradient(90deg, ${linesColor} ${lineThickness}px, transparent ${lineThickness}px)
            `,
            backgroundSize: '60px 60px',
            opacity: 0.5,
          }}
        />

        {/* Center Fade Overlay - Creates the dark tunnel center */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'radial-gradient(ellipse at center, rgba(0,0,0,0.9) 0%, transparent 70%)',
            pointerEvents: 'none',
          }}
        />

        {/* Scan Line */}
        <div
          style={{
            position: 'absolute',
            left: 0,
            right: 0,
            height: `${scanSoftness * 50}px`,
            background: `linear-gradient(to bottom, transparent, ${scanColor}${Math.round(scanOpacity * 255).toString(16).padStart(2, '0')}, transparent)`,
            boxShadow: scanGlow > 0 ? `0 0 ${scanGlow * 30}px ${scanGlow * 15}px ${scanColor}${Math.round(scanOpacity * 0.5 * 255).toString(16).padStart(2, '0')}` : 'none',
            animation: `scanMove ${scanDuration * 2}s ease-in-out ${scanDelay}s infinite`,
            pointerEvents: 'none',
          }}
        />
      </div>

      {/* Edge Vignette */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'radial-gradient(ellipse at center, transparent 30%, rgba(0,0,0,0.8) 100%)',
          pointerEvents: 'none',
        }}
      />
    </div>
  )
}
