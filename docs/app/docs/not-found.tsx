'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { wildcardRedirects } from '../../redirects.config'

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

    // Determine which version we're in and redirect to overview
    if (pathname.includes('/docs/v1')) {
      router.replace('/docs/v1/introduction/overview')
      return
    }
    if (pathname.includes('/docs/v2')) {
      router.replace('/docs/v2/introduction/overview')
      return
    }

    // Default to V2 overview
    router.replace('/docs/v2/introduction/overview')
  }, [router, pathname])

  if (isRedirecting) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        flexDirection: 'column',
        gap: '1rem'
      }}>
        <h1>Redirecting...</h1>
        <p>Please wait while we redirect you to the right page.</p>
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
      gap: '1rem'
    }}>
      <h1>Page Not Found</h1>
      <p>This page doesn't exist in this version.</p>
      <a href="/docs/v2/introduction/overview" style={{ color: '#0070f3' }}>
        Go to Documentation
      </a>
    </div>
  )
}
