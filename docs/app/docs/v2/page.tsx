'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function V2Index() {
  const router = useRouter()

  useEffect(() => {
    router.replace('/docs/v2/introduction/overview')
  }, [router])

  return null
}
