'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function V3Index() {
  const router = useRouter()

  useEffect(() => {
    router.replace('/docs/v3/introduction/overview')
  }, [router])

  return null
}
