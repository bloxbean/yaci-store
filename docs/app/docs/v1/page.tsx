'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function V1Index() {
  const router = useRouter()

  useEffect(() => {
    router.replace('/docs/v1/introduction/overview')
  }, [router])

  return null
}
