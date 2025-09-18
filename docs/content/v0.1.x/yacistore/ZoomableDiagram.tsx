'use client'

import React, { useState } from 'react'

interface ZoomableDiagramProps {
  children: React.ReactNode
}

export default function ZoomableDiagram({ children }: ZoomableDiagramProps) {
  const [scale, setScale] = useState(1.0)

  const handleZoomIn = () => {
    setScale(prevScale => Math.min(prevScale + 0.2, 3))
  }

  const handleZoomOut = () => {
    setScale(prevScale => Math.max(prevScale - 0.2, 0.5))
  }

  const handleReset = () => {
    setScale(1.0)
  }

  return (
    <div style={{
      background: 'linear-gradient(135deg, rgba(123, 31, 162, 0.05) 0%, rgba(156, 39, 176, 0.05) 100%)',
      border: '1px solid rgba(123, 31, 162, 0.1)',
      borderRadius: '12px',
      padding: '1rem',
      marginBottom: '2rem'
    }}>
      {/* Zoom Controls */}
      <div style={{
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center',
        marginBottom: '1rem',
        padding: '0.5rem',
        background: 'rgba(255, 255, 255, 0.7)',
        borderRadius: '8px',
        border: '1px solid rgba(123, 31, 162, 0.2)'
      }}>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <button
            onClick={handleZoomOut}
            style={{
              background: 'rgba(123, 31, 162, 0.1)',
              border: '1px solid rgba(123, 31, 162, 0.3)',
              borderRadius: '6px',
              padding: '0.25rem 0.5rem',
              cursor: 'pointer',
              fontSize: '0.8rem',
              color: '#333'
            }}
          >
            🔍−
          </button>
          <span style={{ 
            fontSize: '0.8rem', 
            color: '#666',
            minWidth: '40px',
            textAlign: 'center'
          }}>
            {Math.round(scale * 100)}%
          </span>
          <button
            onClick={handleZoomIn}
            style={{
              background: 'rgba(123, 31, 162, 0.1)',
              border: '1px solid rgba(123, 31, 162, 0.3)',
              borderRadius: '6px',
              padding: '0.25rem 0.5rem',
              cursor: 'pointer',
              fontSize: '0.8rem',
              color: '#333'
            }}
          >
            🔍+
          </button>
          <button
            onClick={handleReset}
            style={{
              background: 'rgba(123, 31, 162, 0.1)',
              border: '1px solid rgba(123, 31, 162, 0.3)',
              borderRadius: '6px',
              padding: '0.25rem 0.5rem',
              cursor: 'pointer',
              fontSize: '0.8rem',
              color: '#333'
            }}
          >
            Reset
          </button>
        </div>
      </div>

      {/* Scrollable Diagram Container */}
      <div style={{
        overflow: 'auto',
        maxHeight: '90vh',
        border: '1px solid rgba(123, 31, 162, 0.2)',
        borderRadius: '8px',
        background: 'white',
        cursor: scale > 1 ? 'grab' : 'default'
      }}>
        <div style={{ 
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
          minWidth: '100%',
          padding: '1rem',
          transition: 'transform 0.2s ease'
        }}>
          {children}
        </div>
      </div>
    </div>
  )
}