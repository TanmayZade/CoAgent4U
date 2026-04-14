import { QueryProvider } from '@/components/query-provider'
import { ThemeProvider } from '@/components/theme-provider'
import { Toaster } from '@/components/ui/sonner'
import { Analytics } from '@vercel/analytics/next'
import { SpeedInsights } from '@vercel/speed-insights/next'
import type { Metadata, Viewport } from 'next'
import { JetBrains_Mono, Playfair_Display, Poppins } from 'next/font/google'
import './globals.css'

const poppins = Poppins({
  subsets: ["latin"],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-sans',
  display: 'swap',
})

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: '--font-mono',
  display: 'swap',
})

// The Seasons-style elegant serif font for brand name
const playfairDisplay = Playfair_Display({
  subsets: ["latin"],
  weight: ['400', '500', '600'],
  variable: '--font-serif',
  display: 'swap',
})

export const metadata: Metadata = {
  title: 'CoAgent4U | Your Personal AI Agent Powered by A2A & MCP',
  description: 'A privacy-centric personal AI agent that manages your calendar, tasks, and productivity through MCP tools and coordinates with other agents via the A2A protocol. Human-in-the-loop control.',
  keywords: ['personal agent', 'MCP', 'A2A', 'Model Context Protocol', 'Agent-to-Agent', 'AI agent', 'calendar', 'scheduling', 'productivity', 'LLM'],
  icons: {
    icon: [
      {
        url: '/images/logo-light.png',
        media: '(prefers-color-scheme: light)',
      },
      {
        url: '/images/logo-dark.png',
        media: '(prefers-color-scheme: dark)',
      },
    ],
    apple: '/images/logo-light.png',
  },
}

export const viewport: Viewport = {
  themeColor: '#FFFFFF',
  width: 'device-width',
  initialScale: 1,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${poppins.variable} ${jetbrainsMono.variable} ${playfairDisplay.variable} font-sans antialiased bg-background text-foreground transition-colors duration-300`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem={true}
          disableTransitionOnChange={false}
        >
          <QueryProvider>
            {children}
          </QueryProvider>
        </ThemeProvider>
        <Analytics />
        <SpeedInsights />
        <Toaster />
      </body>
    </html>
  )
}
