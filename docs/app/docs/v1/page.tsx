'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function V1Index() {
  const router = useRouter()

  useEffect(() => {
    router.replace('/docs/v1/yaci-store/overview')
  }, [router])

  return null
}
