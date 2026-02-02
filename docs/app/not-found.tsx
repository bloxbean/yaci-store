'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { wildcardRedirects } from '../redirects.config'

export default function NotFound() {
  const router = useRouter()
  const pathname = usePathname()
  const [isRedirecting, setIsRedirecting] = useState(true)

  useEffect(() => {
    if (!pathname) {
      setIsRedirecting(false)
      return
    }

    // Check wildcard redirects
    for (const { pattern, replacement } of wildcardRedirects) {
      if (pattern.test(pathname)) {
        const newPath = pathname.replace(pattern, replacement)
        router.replace(newPath)
        return
      }
    }

    // If in docs path but no redirect matched, go to overview
    if (pathname.includes('/docs/v1')) {
      router.replace('/docs/v1/introduction/overview')
      return
    }
    if (pathname.includes('/docs/v2')) {
      router.replace('/docs/v2/introduction/overview')
      return
    }
    if (pathname.startsWith('/docs')) {
      router.replace('/docs/v2/introduction/overview')
      return
    }

    // No redirect found
    setIsRedirecting(false)
  }, [router, pathname])

  if (isRedirecting) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        flexDirection: 'column',
        gap: '1rem',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, sans-serif',
        background: '#f5f5f5'
      }}>
        <h1 style={{ margin: 0, fontSize: '1.5rem' }}>Redirecting...</h1>
        <p style={{ margin: 0, color: '#666' }}>Please wait while we redirect you to the right page.</p>
      </div>
    )
  }

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100vh',
      flexDirection: 'column',
      gap: '1rem',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, sans-serif',
      background: '#f5f5f5'
    }}>
      <h1 style={{ margin: 0, fontSize: '2rem' }}>404 - Page Not Found</h1>
      <p style={{ margin: 0, color: '#666' }}>The page you're looking for doesn't exist.</p>
      <a
        href="/docs/v2/introduction/overview"
        style={{
          marginTop: '1rem',
          color: '#0070f3',
          textDecoration: 'none'
        }}
      >
        Go to Documentation
      </a>
    </div>
  )
}
