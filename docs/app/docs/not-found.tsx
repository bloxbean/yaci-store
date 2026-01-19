'use client'

import { useEffect } from 'react'
import { useRouter, usePathname } from 'next/navigation'

export default function NotFound() {
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    // Determine which version we're in
    const isV1 = pathname?.includes('/docs/v1')
    const isV2 = pathname?.includes('/docs/v2')

    // Redirect to the appropriate version's overview
    if (isV1) {
      router.replace('/docs/v1/yaci-store/overview')
    } else if (isV2) {
      router.replace('/docs/v2/yaci-store/overview')
    } else {
      // Default to V2 overview
      router.replace('/docs/v2/yaci-store/overview')
    }
  }, [router, pathname])

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
      <p>This page doesn't exist in this version. Redirecting to overview...</p>
    </div>
  )
}
