'use client'

import React, { useEffect, useState } from 'react'

export default function ZoomableImage(props) {
  const { src, alt, style, ...rest } = props
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) return
    const onKeyDown = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open])

  return (
    <>
      <img
        src={src}
        alt={alt}
        onClick={() => setOpen(true)}
        style={{
          display: 'block',
          cursor: 'zoom-in',
          maxWidth: '100%',
          height: 'auto',
          margin: '1.5rem 0',
          ...style
        }}
        {...rest}
      />

      {open && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={alt || 'Zoomed image'}
          onClick={() => setOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.85)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
            padding: '2rem'
          }}
        >
          <img
            src={src}
            alt={alt}
            onClick={(e) => e.stopPropagation()}
            style={{
              maxWidth: '90vw',
              maxHeight: '90vh',
              boxShadow: '0 10px 30px rgba(0,0,0,0.6)',
              borderRadius: '8px'
            }}
          />
          <button
            onClick={() => setOpen(false)}
            aria-label="Close"
            style={{
              position: 'fixed',
              top: '16px',
              right: '16px',
              background: 'rgba(255,255,255,0.85)',
              border: 'none',
              borderRadius: '6px',
              padding: '8px 10px',
              cursor: 'pointer',
              fontSize: '14px'
            }}
          >
            ✕
          </button>
        </div>
      )}
    </>
  )
}

