"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import YaciStore from "../utils/icons/YaciStore.svg";

export default function LandingPage() {
  const [mounted, setMounted] = useState(false);
  const [bannerVisible, setBannerVisible] = useState(true);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return null;
  }

  return (
    <>
      <style jsx global>{`
        .devkit-landing {
          min-height: 100vh;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Inter", sans-serif;
          background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%);
          color: #ffffff;
          overflow-x: hidden;
        }

        .animated-background {
          position: fixed;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          background: 
            radial-gradient(circle at 25% 25%, rgba(139, 92, 246, 0.15) 0%, transparent 50%),
            radial-gradient(circle at 75% 75%, rgba(56, 189, 248, 0.15) 0%, transparent 50%),
            radial-gradient(circle at 50% 50%, rgba(34, 197, 94, 0.1) 0%, transparent 50%);
          animation: backgroundFloat 20s ease-in-out infinite;
          z-index: -1;
        }

        @keyframes backgroundFloat {
          0%, 100% { 
            transform: translateY(0px) rotate(0deg); 
            opacity: 1;
          }
          50% { 
            transform: translateY(-20px) rotate(2deg); 
            opacity: 0.8;
          }
        }

        .container {
          max-width: 1400px;
          margin: 0 auto;
          padding: 0 2rem;
          position: relative;
          z-index: 1;
        }

        /* Hero Section */
        .hero-section {
          min-height: 100vh;
          display: flex;
          align-items: center;
          position: relative;
          padding: 6rem 0;
        }

        /* Hero Main Row - Side by Side */
        .hero-main-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 3rem;
          align-items: center;
          margin-bottom: 3rem;
        }

        .hero-main-text {
          display: flex;
          flex-direction: column;
          gap: 1.5rem;
        }

        .hero-mini-architecture {
          display: flex;
          justify-content: center;
          align-items: center;
          padding: 0;
        }

        .brand-header {
          display: flex;
          align-items: center;
          gap: 2rem;
          margin-bottom: 2rem;
        }

        .logo-container {
          position: relative;
        }

        .logo-glow {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: 120px;
          height: 120px;
          background: radial-gradient(circle, rgba(56, 189, 248, 0.3) 0%, transparent 70%);
          border-radius: 50%;
          animation: logoGlow 3s ease-in-out infinite;
        }

        @keyframes logoGlow {
          0%, 100% { 
            opacity: 0.3; 
            transform: translate(-50%, -50%) scale(1); 
          }
          50% { 
            opacity: 0.6; 
            transform: translate(-50%, -50%) scale(1.1); 
          }
        }

        .brand-title {
          font-size: 4rem;
          font-weight: 800;
          margin: 0;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #667eea 100%);
          background-clip: text;
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          line-height: 1.1;
        }

        .hero-subtitle {
          font-size: 1.8rem;
          color: #cbd5e1;
          font-weight: 300;
          margin-bottom: 1rem;
          line-height: 1.3;
        }

        .hero-description {
          font-size: 1.2rem;
          color: #94a3b8;
          line-height: 1.6;
          margin-bottom: 2.5rem;
        }

        .hero-buttons {
          display: flex;
          gap: 1rem;
          flex-wrap: wrap;
        }

        .btn {
          padding: 0.875rem 2rem;
          border-radius: 8px;
          text-decoration: none;
          font-weight: 600;
          font-size: 1rem;
          transition: all 0.3s ease;
          display: inline-flex;
          align-items: center;
          gap: 0.5rem;
          border: 2px solid transparent;
          cursor: pointer;
        }

        .btn-primary {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #ffffff;
        }

        .btn-primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
        }

        .btn-secondary {
          background: transparent;
          color: #667eea;
          border-color: #667eea;
        }

        .btn-secondary:hover {
          background: #667eea;
          color: #ffffff;
          transform: translateY(-2px);
        }

        /* Enhanced Architecture */
        .enhanced-arch-container {
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.95) 0%, rgba(30, 41, 59, 0.95) 100%);
          border-radius: 12px;
          padding: 1.25rem;
          backdrop-filter: blur(15px);
          max-width: 320px;
          width: 100%;
          position: relative;
          overflow: hidden;
        }

        .enhanced-arch-container::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: 
            radial-gradient(circle at 30% 20%, rgba(102, 126, 234, 0.1) 0%, transparent 50%),
            radial-gradient(circle at 70% 80%, rgba(118, 75, 162, 0.1) 0%, transparent 50%);
          pointer-events: none;
        }


        .enhanced-flow-diagram {
          display: flex;
          flex-direction: column;
          gap: 0.75rem;
          position: relative;
          z-index: 1;
        }

        .enhanced-flow-node {
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.15) 100%);
          border: 1px solid rgba(102, 126, 234, 0.4);
          border-radius: 10px;
          padding: 0.75rem;
          text-align: center;
          transition: all 0.3s ease;
          position: relative;
          overflow: hidden;
        }

        .enhanced-flow-node:hover {
          transform: translateY(-3px);
          border-color: rgba(102, 126, 234, 0.7);
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
        }

        .node-glow {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          width: 100%;
          height: 100%;
          background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
          border-radius: 12px;
          opacity: 0;
          transition: opacity 0.3s ease;
        }

        .enhanced-flow-node:hover .node-glow {
          opacity: 1;
        }

        .node-icon {
          font-size: 1.4rem;
          margin-bottom: 0.3rem;
          display: block;
          position: relative;
          z-index: 1;
        }

        .node-title {
          font-size: 0.85rem;
          font-weight: 600;
          color: #ffffff;
          margin-bottom: 0.4rem;
          position: relative;
          z-index: 1;
        }

        .node-details {
          display: flex;
          gap: 0.5rem;
          justify-content: center;
          flex-wrap: wrap;
          position: relative;
          z-index: 1;
        }

        .detail-item {
          background: rgba(102, 126, 234, 0.2);
          color: #cbd5e1;
          padding: 0.15rem 0.4rem;
          border-radius: 5px;
          font-size: 0.65rem;
          font-weight: 500;
          border: 1px solid rgba(102, 126, 234, 0.3);
        }

        .enhanced-flow-connector {
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
          height: 30px;
        }

        .connector-line {
          width: 2px;
          height: 20px;
          background: linear-gradient(180deg, #667eea, #764ba2);
          border-radius: 1px;
          position: relative;
        }

        .data-flow {
          position: absolute;
          width: 6px;
          height: 6px;
          background: #667eea;
          border-radius: 50%;
          animation: dataFlowVertical 2s ease-in-out infinite;
          box-shadow: 0 0 8px rgba(102, 126, 234, 0.6);
        }

        @keyframes dataFlowVertical {
          0% { 
            transform: translateY(-15px);
            opacity: 0;
          }
          50% { 
            opacity: 1;
          }
          100% { 
            transform: translateY(15px);
            opacity: 0;
          }
        }

        .connector-arrow {
          position: absolute;
          font-size: 1.2rem;
          color: #667eea;
          bottom: -5px;
          transform: rotate(90deg);
          animation: arrowPulse 2s ease-in-out infinite;
        }

        .processing-indicator {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 3px;
          background: linear-gradient(90deg, transparent, #22c55e, transparent);
          animation: processing 3s ease-in-out infinite;
          border-radius: 12px 12px 0 0;
        }

        @keyframes processing {
          0%, 100% { transform: translateX(-100%); }
          50% { transform: translateX(100%); }
        }

        .networks { border-color: rgba(34, 197, 94, 0.4); }
        .networks .detail-item { background: rgba(34, 197, 94, 0.2); border-color: rgba(34, 197, 94, 0.3); }

        .processing { border-color: rgba(139, 92, 246, 0.4); }
        .processing .detail-item { background: rgba(139, 92, 246, 0.2); border-color: rgba(139, 92, 246, 0.3); }

        .storage { border-color: rgba(59, 130, 246, 0.4); }
        .storage .detail-item { background: rgba(59, 130, 246, 0.2); border-color: rgba(59, 130, 246, 0.3); }

        .access { border-color: rgba(245, 158, 11, 0.4); }
        .access .detail-item { background: rgba(245, 158, 11, 0.2); border-color: rgba(245, 158, 11, 0.3); }

        .arch-stats {
          display: flex;
          justify-content: space-between;
          margin-top: 1rem;
          padding-top: 0.5rem;
          position: relative;
          z-index: 1;
        }

        .stat-item {
          text-align: center;
          flex: 1;
        }

        .stat-value {
          font-size: 0.9rem;
          font-weight: 700;
          color: #667eea;
          display: block;
        }

        .stat-label {
          font-size: 0.6rem;
          color: #94a3b8;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          margin-top: 0.2rem;
        }

        .floating-card {
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.9) 100%);
          border: 1px solid rgba(102, 126, 234, 0.3);
          border-radius: 12px;
          padding: 1.5rem;
          backdrop-filter: blur(10px);
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
          transition: all 0.3s ease;
          text-align: center;
        }

        .floating-card:hover {
          transform: translateY(-5px);
          border-color: rgba(102, 126, 234, 0.6);
          box-shadow: 0 12px 40px rgba(102, 126, 234, 0.2);
        }

        .card-icon {
          font-size: 2rem;
          margin-bottom: 0.5rem;
          display: block;
        }

        .card-title {
          font-size: 1rem;
          font-weight: 600;
          color: #ffffff;
          margin-bottom: 0.5rem;
        }

        .card-desc {
          font-size: 0.9rem;
          color: #94a3b8;
          line-height: 1.4;
        }

        /* Features Grid Section */
        .features-section {
          padding: 4rem 0;
          position: relative;
        }

        .section-header {
          text-align: center;
          margin-bottom: 4rem;
        }

        .section-title {
          font-size: 3rem;
          font-weight: 700;
          margin-bottom: 1rem;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          background-clip: text;
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }

        .section-subtitle {
          font-size: 1.2rem;
          color: #94a3b8;
          max-width: 600px;
          margin: 0 auto;
          line-height: 1.6;
        }

        .features-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
          gap: 2rem;
          margin-bottom: 4rem;
        }

        .feature-card {
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.8) 0%, rgba(30, 41, 59, 0.8) 100%);
          border: 1px solid rgba(102, 126, 234, 0.2);
          border-radius: 16px;
          padding: 2.5rem;
          backdrop-filter: blur(10px);
          transition: all 0.3s ease;
          position: relative;
          overflow: hidden;
        }

        .feature-card::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 3px;
          background: linear-gradient(90deg, #667eea, #764ba2, #f093fb);
          opacity: 0;
          transition: opacity 0.3s ease;
        }

        .feature-card:hover {
          transform: translateY(-10px);
          border-color: rgba(102, 126, 234, 0.5);
          box-shadow: 0 20px 50px rgba(102, 126, 234, 0.15);
        }

        .feature-card:hover::before {
          opacity: 1;
        }

        .feature-header {
          display: flex;
          align-items: center;
          gap: 1rem;
          margin-bottom: 1.5rem;
        }

        .feature-icon {
          font-size: 2.5rem;
          flex-shrink: 0;
        }

        .feature-title {
          font-size: 1.4rem;
          font-weight: 600;
          color: #ffffff;
          margin: 0;
        }

        .feature-description {
          color: #cbd5e1;
          line-height: 1.6;
          margin-bottom: 1.5rem;
          font-size: 1rem;
        }

        .feature-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .feature-list li {
          color: #94a3b8;
          padding: 0.4rem 0;
          position: relative;
          padding-left: 1.5rem;
          font-size: 0.95rem;
        }

        .feature-list li::before {
          content: '✓';
          color: #22c55e;
          position: absolute;
          left: 0;
          font-weight: bold;
        }

        /* Distributions Section */
        .distributions-section {
          padding: 4rem 0;
          margin: 0;
        }

        .distributions-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
          gap: 2rem;
          margin-bottom: 4rem;
        }

        .distribution-card {
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.8) 0%, rgba(30, 41, 59, 0.8) 100%);
          border: 1px solid rgba(102, 126, 234, 0.2);
          border-radius: 16px;
          padding: 2.5rem;
          backdrop-filter: blur(10px);
          transition: all 0.3s ease;
          position: relative;
          overflow: hidden;
        }

        .distribution-card::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 3px;
          background: linear-gradient(90deg, #667eea, #764ba2, #f093fb);
          opacity: 0;
          transition: opacity 0.3s ease;
        }

        .distribution-card:hover {
          transform: translateY(-10px);
          border-color: rgba(102, 126, 234, 0.5);
          box-shadow: 0 20px 50px rgba(102, 126, 234, 0.15);
        }

        .distribution-card:hover::before {
          opacity: 1;
        }

        .distribution-card.recommended {
          border-color: rgba(34, 197, 94, 0.4);
          box-shadow: 0 10px 30px rgba(34, 197, 94, 0.1);
        }

        .distribution-card.recommended::before {
          background: linear-gradient(90deg, #22c55e, #16a34a, #15803d);
          opacity: 1;
        }

        .distribution-badge {
          position: absolute;
          top: 1rem;
          right: 1rem;
          background: linear-gradient(135deg, #22c55e, #16a34a);
          color: #ffffff;
          padding: 0.25rem 0.75rem;
          border-radius: 12px;
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .distribution-header {
          display: flex;
          align-items: center;
          gap: 1rem;
          margin-bottom: 1.5rem;
        }

        .distribution-icon {
          font-size: 2.5rem;
          flex-shrink: 0;
        }

        .distribution-title {
          font-size: 1.4rem;
          font-weight: 600;
          color: #ffffff;
          margin: 0;
        }

        .distribution-description {
          color: #cbd5e1;
          line-height: 1.6;
          margin-bottom: 1.5rem;
          font-size: 1rem;
        }

        .distribution-features {
          margin-bottom: 2rem;
        }

        .distribution-features .feature-item {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          padding: 0.5rem 0;
          color: #94a3b8;
          font-size: 0.9rem;
        }

        .distribution-features .feature-icon {
          color: #22c55e;
          font-weight: bold;
          font-size: 1rem;
          flex-shrink: 0;
        }

        .distribution-cta {
          display: flex;
          gap: 1rem;
          flex-wrap: wrap;
        }

        .distribution-cta .btn {
          flex: 1;
          min-width: 120px;
          justify-content: center;
          text-align: center;
        }

        .distributions-footer {
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
          border-radius: 16px;
          padding: 3rem;
          text-align: center;
          border: 1px solid rgba(102, 126, 234, 0.2);
        }

        .footer-title {
          font-size: 1.8rem;
          font-weight: 600;
          color: #ffffff;
          margin-bottom: 1rem;
        }

        .footer-description {
          color: #94a3b8;
          font-size: 1.1rem;
          line-height: 1.6;
          margin-bottom: 2rem;
          max-width: 600px;
          margin-left: auto;
          margin-right: auto;
        }

        .footer-actions {
          display: flex;
          gap: 1rem;
          justify-content: center;
          flex-wrap: wrap;
        }

        /* Trusted By Section */
        .trusted-by-section {
          padding: 4rem 0;
          margin: 0;
        }

        .trusted-projects {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
          gap: 2rem;
          margin-bottom: 4rem;
        }

        .project-card {
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.8) 0%, rgba(30, 41, 59, 0.8) 100%);
          border: 1px solid rgba(102, 126, 234, 0.2);
          border-radius: 16px;
          padding: 2rem;
          backdrop-filter: blur(10px);
          transition: all 0.3s ease;
          display: flex;
          gap: 1.5rem;
          align-items: flex-start;
        }

        .project-card:hover {
          transform: translateY(-5px);
          border-color: rgba(102, 126, 234, 0.4);
          box-shadow: 0 15px 40px rgba(102, 126, 234, 0.15);
        }

        .project-card.featured {
          border-color: rgba(34, 197, 94, 0.4);
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.9) 0%, rgba(22, 101, 52, 0.1) 100%);
        }

        .project-card.featured:hover {
          border-color: rgba(34, 197, 94, 0.6);
          box-shadow: 0 15px 40px rgba(34, 197, 94, 0.2);
        }

        .project-icon {
          font-size: 3rem;
          flex-shrink: 0;
          opacity: 0.9;
        }

        .project-info {
          flex: 1;
        }

        .project-name {
          font-size: 1.3rem;
          font-weight: 600;
          color: #ffffff;
          margin-bottom: 0.75rem;
        }

        .project-card.featured .project-name {
          color: #22c55e;
        }

        .project-description {
          color: #cbd5e1;
          line-height: 1.6;
          margin-bottom: 1rem;
          font-size: 0.95rem;
        }

        .project-links {
          display: flex;
          gap: 1rem;
          flex-wrap: wrap;
        }

        .project-link {
          color: #667eea;
          text-decoration: none;
          font-size: 0.9rem;
          font-weight: 500;
          padding: 0.25rem 0.75rem;
          border-radius: 6px;
          border: 1px solid rgba(102, 126, 234, 0.3);
          background: rgba(102, 126, 234, 0.1);
          transition: all 0.3s ease;
        }

        .project-link:hover {
          background: rgba(102, 126, 234, 0.2);
          border-color: rgba(102, 126, 234, 0.5);
          transform: translateY(-2px);
        }

        .project-link.disabled {
          color: #94a3b8;
          border-color: rgba(148, 163, 184, 0.3);
          background: rgba(148, 163, 184, 0.1);
          cursor: not-allowed;
        }

        .project-link.disabled:hover {
          transform: none;
        }

        .project-card.featured .project-link {
          color: #22c55e;
          border-color: rgba(34, 197, 94, 0.3);
          background: rgba(34, 197, 94, 0.1);
        }

        .project-card.featured .project-link:hover {
          background: rgba(34, 197, 94, 0.2);
          border-color: rgba(34, 197, 94, 0.5);
        }

        .trusted-stats {
          display: flex;
          justify-content: center;
          gap: 4rem;
          padding-top: 3rem;
          border-top: 1px solid rgba(102, 126, 234, 0.2);
          flex-wrap: wrap;
        }

        .trust-stat {
          text-align: center;
        }

        .trust-stat .stat-number {
          font-size: 2.5rem;
          font-weight: 700;
          color: #667eea;
          display: block;
          margin-bottom: 0.5rem;
        }

        .trust-stat .stat-label {
          color: #94a3b8;
          font-size: 0.9rem;
          text-transform: uppercase;
          letter-spacing: 0.5px;
          font-weight: 500;
        }

        /* Top Header Banner */
        .top-header-banner {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          background: #ffffff;
          border-bottom: 1px solid #e5e7eb;
          box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
          z-index: 1000;
          padding: 0.75rem 0;
        }

        .banner-content {
          max-width: 1400px;
          margin: 0 auto;
          padding: 0 2rem;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 1rem;
        }

        .banner-left {
          display: flex;
          align-items: center;
          gap: 0.75rem;
        }

        .banner-brand {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          text-decoration: none;
          color: inherit;
        }

        .banner-logo {
          flex-shrink: 0;
        }

        .banner-brand-text {
          font-size: 1.1rem;
          font-weight: 700;
          color: #1f2937;
          margin: 0;
        }

        .banner-right {
          display: flex;
          align-items: center;
          gap: 1.5rem;
        }

        .banner-nav-links {
          display: flex;
          align-items: center;
          gap: 1.5rem;
        }

        .banner-text {
          color: #374151;
          font-size: 0.9rem;
          font-weight: 500;
          margin: 0;
        }

        .banner-link {
          color: #3b82f6;
          text-decoration: none;
          font-weight: 600;
          font-size: 0.9rem;
          transition: color 0.2s ease;
        }

        .banner-link:hover {
          color: #1d4ed8;
          text-decoration: underline;
        }

        .banner-nav-link {
          color: #374151;
          text-decoration: none;
          font-size: 0.85rem;
          font-weight: 500;
          padding: 0.5rem 0.75rem;
          border-radius: 6px;
          transition: all 0.2s ease;
          white-space: nowrap;
        }

        .banner-nav-link:hover {
          color: #3b82f6;
          background-color: #f8fafc;
        }

        .banner-nav-link.primary {
          background-color: #3b82f6;
          color: #ffffff;
        }

        .banner-nav-link.primary:hover {
          background-color: #2563eb;
          color: #ffffff;
        }

        .banner-close {
          background: none;
          border: none;
          color: #6b7280;
          cursor: pointer;
          padding: 0.25rem;
          border-radius: 4px;
          transition: background-color 0.2s ease;
          font-size: 1rem;
          margin-left: 1rem;
        }

        .banner-close:hover {
          background-color: #f3f4f6;
          color: #374151;
        }

        .devkit-landing.with-banner {
          padding-top: 60px;
        }

        /* Community Section */
        .community-section {
          padding: 4rem 0;
          background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
          border-radius: 24px;
          margin: 0;
          text-align: center;
        }

        .community-title {
          font-size: 2.5rem;
          font-weight: 700;
          margin-bottom: 1.5rem;
          color: #ffffff;
        }

        .community-desc {
          font-size: 1.2rem;
          color: #94a3b8;
          margin-bottom: 3rem;
          max-width: 600px;
          margin-left: auto;
          margin-right: auto;
        }

        .community-links {
          display: flex;
          justify-content: center;
          gap: 2rem;
          flex-wrap: wrap;
        }

        .community-link {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 1rem 2rem;
          background: linear-gradient(135deg, rgba(15, 23, 42, 0.8) 0%, rgba(30, 41, 59, 0.8) 100%);
          border: 1px solid rgba(102, 126, 234, 0.3);
          border-radius: 12px;
          color: #ffffff;
          text-decoration: none;
          transition: all 0.3s ease;
          font-weight: 500;
        }

        .community-link:hover {
          transform: translateY(-3px);
          border-color: rgba(102, 126, 234, 0.6);
          box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
        }

        .community-icon {
          font-size: 1.5rem;
        }

        /* What's New in Beta 3 Styles */
        .whats-new-section {
          padding: 5rem 0;
          background: linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(168, 85, 247, 0.05) 100%);
          border-top: 1px solid rgba(99, 102, 241, 0.1);
        }

        .whats-new-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 2rem;
          margin: 3rem 0;
        }

        .feature-card {
          background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
          backdrop-filter: blur(10px);
          border: 1px solid rgba(99, 102, 241, 0.2);
          border-radius: 16px;
          padding: 2rem;
          transition: all 0.3s ease;
          position: relative;
          overflow: hidden;
        }

        .feature-card:hover {
          transform: translateY(-8px);
          box-shadow: 0 20px 40px rgba(99, 102, 241, 0.15);
          border-color: rgba(99, 102, 241, 0.4);
          background: linear-gradient(135deg, rgba(99, 102, 241, 0.15) 0%, rgba(168, 85, 247, 0.15) 100%);
        }

        .feature-card.highlight {
          border: 2px solid rgba(99, 102, 241, 0.3);
          background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(168, 85, 247, 0.1) 100%);
        }

        .feature-card.highlight::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          height: 3px;
          background: linear-gradient(90deg, #6366f1, #a855f7);
        }

        .feature-icon {
          font-size: 2.5rem;
          margin-bottom: 1rem;
          display: block;
        }

        .feature-content {
          position: relative;
        }

        .feature-title {
          font-size: 1.25rem;
          font-weight: 700;
          color: #1e293b;
          margin-bottom: 0.75rem;
          line-height: 1.4;
        }

        .feature-description {
          color: #64748b;
          line-height: 1.6;
          margin-bottom: 1rem;
        }

        .feature-badge {
          position: absolute;
          top: -2rem;
          right: -2rem;
          background: linear-gradient(135deg, #6366f1, #a855f7);
          color: white;
          padding: 0.25rem 0.75rem;
          border-radius: 12px;
          font-size: 0.75rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        .whats-new-footer {
          margin-top: 4rem;
          padding-top: 2rem;
          border-top: 1px solid rgba(99, 102, 241, 0.1);
        }

        .release-info {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 2rem;
        }

        .release-meta {
          display: flex;
          align-items: center;
          gap: 1rem;
          flex-wrap: wrap;
        }

        .release-version {
          background: linear-gradient(135deg, #6366f1, #a855f7);
          color: white;
          padding: 0.5rem 1rem;
          border-radius: 20px;
          font-weight: 600;
          font-size: 0.9rem;
        }

        .release-date {
          color: #64748b;
          font-weight: 500;
        }

        .release-status {
          background: rgba(34, 197, 94, 0.1);
          color: #059669;
          padding: 0.25rem 0.75rem;
          border-radius: 12px;
          font-weight: 600;
          font-size: 0.85rem;
          border: 1px solid rgba(34, 197, 94, 0.2);
        }

        .release-actions {
          display: flex;
          gap: 1rem;
          align-items: center;
        }

        /* Responsive Design */
        @media (max-width: 1024px) {
          .hero-main-row {
            grid-template-columns: 1fr;
            gap: 2rem;
            text-align: center;
          }

          .hero-mini-architecture {
            justify-content: center;
            padding-top: 0.5rem;
          }

          .enhanced-arch-container {
            margin: 0 auto;
            max-width: 350px;
          }

          .distributions-grid {
            grid-template-columns: 1fr;
          }


          .footer-actions {
            flex-direction: column;
            align-items: center;
          }
        }

        @media (max-width: 768px) {
          .whats-new-grid {
            grid-template-columns: 1fr;
            gap: 1.5rem;
          }

          .feature-card {
            padding: 1.5rem;
          }

          .release-info {
            flex-direction: column;
            align-items: flex-start;
            gap: 1.5rem;
          }

          .release-actions {
            width: 100%;
            justify-content: flex-start;
          }

          .container {
            padding: 0 1rem;
          }

          .brand-header {
            flex-direction: column;
            text-align: center;
            gap: 1rem;
          }

          .brand-title {
            font-size: 2.5rem;
          }

          .hero-subtitle {
            font-size: 1.4rem;
          }

          .section-title {
            font-size: 2rem;
          }

          .features-grid {
            grid-template-columns: 1fr;
          }

          .distributions-section {
            padding: 3rem 0;
          }

          .distribution-card {
            padding: 2rem;
          }

          .distribution-cta {
            flex-direction: column;
          }

          .distributions-footer {
            padding: 2rem;
          }


          .enhanced-arch-container {
            padding: 1.5rem;
            max-width: 350px;
          }

          .arch-stats {
            flex-direction: column;
            gap: 0.5rem;
          }

          .stat-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.25rem 0;
          }

          .stat-value {
            font-size: 1rem;
          }

          .floating-card {
            padding: 1rem;
          }

          .hero-buttons {
            justify-content: center;
          }
        }

        @media (max-width: 768px) {
          .banner-content {
            flex-direction: column;
            gap: 0.75rem;
            padding: 0 1rem;
          }

          .banner-nav-links {
            gap: 1rem;
          }

          .banner-nav-link {
            font-size: 0.8rem;
            padding: 0.4rem 0.6rem;
          }

          .banner-brand-text {
            font-size: 1rem;
          }

          .banner-logo img {
            height: 28px;
            width: 28px;
          }
        }

        @media (max-width: 480px) {
          .hero-section {
            padding: 4rem 0;
          }

          .brand-title {
            font-size: 2rem;
          }

          .hero-subtitle {
            font-size: 1.2rem;
          }

          .enhanced-arch-container {
            padding: 1.25rem;
            max-width: 300px;
          }

          .arch-title {
            font-size: 1.1rem;
          }

          .arch-subtitle {
            font-size: 0.75rem;
          }

          .node-title {
            font-size: 0.85rem;
          }

          .detail-item {
            font-size: 0.65rem;
            padding: 0.15rem 0.4rem;
          }
        }
      `}</style>

      <div className={`devkit-landing ${bannerVisible ? 'with-banner' : ''}`}>
        <div className="animated-background"></div>
        
        {/* Top Header Banner */}
        {bannerVisible && (
          <div className="top-header-banner">
            <div className="banner-content">
              <div className="banner-left">
                <Link href="/" className="banner-brand">
                  <div className="banner-logo">
                    <Image
                      src={YaciStore}
                      alt="Yaci Store Logo"
                      height={32}
                      width={32}
                    />
                  </div>
                  <h1 className="banner-brand-text">Yaci Store</h1>
                </Link>
              </div>
              
              <div className="banner-right">
                <nav className="banner-nav-links">
                  <Link href="/docs/v2.0.0/gettingStarted/installation/run" className="banner-nav-link primary">
                    Get Started
                  </Link>
                  <Link href="/docs/v2.0.0" className="banner-nav-link">
                    Documentation
                  </Link>
                  <a href="https://github.com/bloxbean/yaci-store" target="_blank" className="banner-nav-link">
                    GitHub
                  </a>
                  <a href="https://discord.gg/6AnKrFx4ne" target="_blank" className="banner-nav-link">
                    Discord
                  </a>
                </nav>
                
                <button 
                  className="banner-close" 
                  onClick={() => setBannerVisible(false)}
                  aria-label="Close banner"
                >
                  ×
                </button>
              </div>
            </div>
          </div>
        )}
        
        <div className="container">
          {/* Hero Section */}
          <section className="hero-section">
            {/* Main Content Row - Side by Side */}
            <div className="hero-main-row">
              <div className="hero-main-text">
                <div className="brand-header">
                  <div className="logo-container">
                    <div className="logo-glow"></div>
                    <Image
                      src={YaciStore}
                      alt="Yaci Store Logo"
                      height={80}
                      style={{ position: "relative", zIndex: 2 }}
                    />
                  </div>
                  <h1 className="brand-title">Yaci Store</h1>
                </div>
                
                <h2 className="hero-subtitle">
                  The ultimate Cardano blockchain indexer
                </h2>
                
                <p className="hero-description">
                  <strong>Get started in minutes, not days.</strong> Deploy with Docker, integrate as a Java library, or run standalone. Choose exactly what you need with modular stores, filter data granularly with plugins (JavaScript, Python, MVEL), access through comprehensive REST APIs, and build production-ready applications with enterprise-grade performance.
                </p>
                

                <div className="hero-buttons">
                  <Link href="/docs/v2.0.0/gettingStarted/installation/run" className="btn btn-primary">
                    🚀 Get Started
                  </Link>
                  <Link 
                    href="/docs/v2.0.0" 
                    className="btn btn-secondary"
                  >
                    📚 Documentation
                  </Link>
                </div>
              </div>

              <div className="hero-mini-architecture">
                {/* Enhanced Architecture Flow Diagram */}
                <div className="enhanced-arch-container">

                  <div className="enhanced-flow-diagram">
                    <div className="flow-step">
                      <div className="enhanced-flow-node networks">
                        <div className="node-glow"></div>
                        <div className="node-icon">₳</div>
                        <div className="node-title">Cardano Networks</div>
                        <div className="node-details">
                          <div className="detail-item">Mainnet</div>
                          <div className="detail-item">Preview</div>
                          <div className="detail-item">Preprod</div>
                        </div>
                      </div>
                    </div>

                    <div className="enhanced-flow-connector">
                      <div className="connector-line"></div>
                      <div className="data-flow"></div>
                      <div className="connector-arrow">→</div>
                    </div>

                    <div className="flow-step">
                      <div className="enhanced-flow-node processing">
                        <div className="node-glow"></div>
                        <div className="node-icon">⚡</div>
                        <div className="node-title">Processing Engine</div>
                        <div className="node-details">
                          <div className="detail-item">Assets Store</div>
                          <div className="detail-item">UTxO Store</div>
                          <div className="detail-item">Blocks Store</div>
                          <div className="detail-item">Transaction Store</div>
                          <div className="detail-item">Governance Store</div>
                          <div className="detail-item">Staking Store</div>
                          <div className="detail-item">Script Store</div>
                          <div className="detail-item">Epoch Store</div>
                          <div className="detail-item">Metadata Store</div>
                          <div className="detail-item">Plugin Framework</div>
                        </div>
                        <div className="processing-indicator"></div>
                      </div>
                    </div>

                    <div className="enhanced-flow-connector">
                      <div className="connector-line"></div>
                      <div className="data-flow"></div>
                      <div className="connector-arrow">→</div>
                    </div>

                    <div className="flow-step">
                      <div className="enhanced-flow-node storage">
                        <div className="node-glow"></div>
                        <div className="node-icon">🗄️</div>
                        <div className="node-title">Indexed Storage</div>
                        <div className="node-details">
                          <div className="detail-item">PostgreSQL</div>
                          <div className="detail-item">H2</div>
                          <div className="detail-item">MySQL</div>
                          <div className="detail-item">Custom</div>
                        </div>
                      </div>
                    </div>

                    <div className="enhanced-flow-connector">
                      <div className="connector-line"></div>
                      <div className="data-flow"></div>
                      <div className="connector-arrow">→</div>
                    </div>

                    <div className="flow-step">
                      <div className="enhanced-flow-node access">
                        <div className="node-glow"></div>
                        <div className="node-icon">🌐</div>
                        <div className="node-title">Access Layer</div>
                        <div className="node-details">
                          <div className="detail-item">REST APIs</div>
                          <div className="detail-item">Java SDK</div>
                          <div className="detail-item">Direct SQL</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div className="arch-stats">
                    <div className="stat-item">
                      <div className="stat-value">95M+</div>
                      <div className="stat-label">Blocks</div>
                    </div>
                    <div className="stat-item">
                      <div className="stat-value">180M+</div>
                      <div className="stat-label">Transactions</div>
                    </div>
                    <div className="stat-item">
                      <div className="stat-value">50+</div>
                      <div className="stat-label">Endpoints</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Features Section */}
          <section className="features-section">
            <div className="section-header">
              <h2 className="section-title">Features</h2>
              <p className="section-subtitle">
                Everything you need to build powerful Cardano applications. Deploy anywhere, index precisely, scale effortlessly from prototype to enterprise-grade solutions.
              </p>
            </div>

            <div className="features-grid">
              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">🚄</div>
                  <h3 className="feature-title">High Performance Indexing</h3>
                </div>
                <p className="feature-description">
                  Built with Java 21 virtual threads and optimized for maximum throughput. Handle millions of transactions with minimal resource usage.
                </p>
                <ul className="feature-list">
                  <li>Virtual thread support for concurrent processing</li>
                  <li>Parallel block processing capabilities</li>
                  <li>Optimized database queries and indexing</li>
                  <li>Selective store activation for efficiency</li>
                </ul>
              </div>

              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">🔧</div>
                  <h3 className="feature-title">Flexible Plugin System</h3>
                </div>
                <p className="feature-description">
                  Extend and customize functionality with a powerful plugin framework supporting multiple scripting languages.
                </p>
                <ul className="feature-list">
                  <li>JavaScript, Python, and MVEL script support</li>
                  <li>Filter and transform data in real-time</li>
                  <li>Event-driven architecture with hooks</li>
                  <li>Custom extension points for any use case</li>
                </ul>
              </div>

              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">🌐</div>
                  <h3 className="feature-title">Comprehensive APIs</h3>
                </div>
                <p className="feature-description">
                  Access blockchain data through REST APIs, Java SDK, or direct database queries with comprehensive endpoint coverage.
                </p>
                <ul className="feature-list">
                  <li>50+ REST API endpoints</li>
                  <li>Native Java Spring Boot integration</li>
                  <li>Direct SQL access for analytics</li>
                  <li>Real-time WebSocket subscriptions</li>
                </ul>
              </div>

              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">🏢</div>
                  <h3 className="feature-title">Production Ready</h3>
                </div>
                <p className="feature-description">
                  Trusted by major Cardano infrastructure providers with enterprise-grade reliability and monitoring capabilities.
                </p>
                <ul className="feature-list">
                  <li>Enterprise-grade architecture with horizontal scaling</li>
                  <li>Built-in health checks and metrics endpoints</li>
                  <li>High availability with automatic failover support</li>
                  <li>Production monitoring with Prometheus integration</li>
                  <li>Zero-downtime deployments and rolling updates</li>
                  <li>Comprehensive logging and error tracking</li>
                </ul>
              </div>

              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">🗄️</div>
                  <h3 className="feature-title">Complete Data Coverage</h3>
                </div>
                <p className="feature-description">
                  Access every piece of Cardano blockchain data with comprehensive indexing. Performs node-level calculations for governance state and reward distributions - nothing is left behind.
                </p>
                <ul className="feature-list">
                  <li>Blocks, transactions, and UTXOs with full metadata</li>
                  <li>Native tokens, NFTs, and asset policies</li>
                  <li>Staking pools, delegations, and calculated rewards</li>
                  <li>Governance proposals, votes, and computed DRep distributions</li>
                  <li>Node-level calculations for treasury and reserves</li>
                  <li>Smart contract interactions and script references</li>
                  <li>Epoch parameters and protocol updates</li>
                </ul>
              </div>

              <div className="feature-card">
                <div className="feature-header">
                  <div className="feature-icon">⚙️</div>
                  <h3 className="feature-title">Easy Deployment</h3>
                </div>
                <p className="feature-description">
                  Get up and running quickly with Docker support, comprehensive documentation, and flexible configuration options.
                </p>
                <ul className="feature-list">
                  <li>One-command Docker deployment</li>
                  <li>Flexible configuration management</li>
                  <li>Network switching (Mainnet/Preview/Preprod)</li>
                  <li>Automated database migrations</li>
                </ul>
              </div>
            </div>
          </section>

          {/* Distributions Section */}
          <section className="distributions-section">
            <div className="section-header">
              <h2 className="section-title">Choose Your Distribution</h2>
              <p className="section-subtitle">
                Deploy anywhere, scale everywhere. Choose your perfect setup and go from zero to indexing in under 5 minutes.
              </p>
            </div>

            <div className="distributions-grid">
              <div className="distribution-card recommended">
                <div className="distribution-badge">Recommended</div>
                <div className="distribution-header">
                  <div className="distribution-icon">🐳</div>
                  <h3 className="distribution-title">Docker Compose</h3>
                </div>
                <p className="distribution-description">
                  <strong>One command. Full stack. Ready to scale.</strong> Complete environment with PostgreSQL that deploys instantly and handles millions of transactions.
                </p>
                <div className="distribution-features">
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>One-command setup</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>PostgreSQL included</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Production ready</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Auto-scaling support</span>
                  </div>
                </div>
                <div className="distribution-cta">
                  <Link href="/docs/v2.0.0/gettingStarted/installation/run" className="btn btn-primary">
                    Quick Start
                  </Link>
                  <a href="https://github.com/bloxbean/yaci-store/tree/main/docker" target="_blank" className="btn btn-secondary">
                    View Docker Files
                  </a>
                </div>
              </div>

              <div className="distribution-card">
                <div className="distribution-header">
                  <div className="distribution-icon">⚙️</div>
                  <h3 className="distribution-title">Standalone JAR</h3>
                </div>
                <p className="distribution-description">
                  Download and run the standalone JAR file with your own database configuration for maximum flexibility.
                </p>
                <div className="distribution-features">
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>No dependencies</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Flexible configuration</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Custom database setup</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Enterprise deployment</span>
                  </div>
                </div>
                <div className="distribution-cta">
                  <a href="https://github.com/bloxbean/yaci-store/releases/latest" target="_blank" className="btn btn-primary">
                    Download Latest
                  </a>
                  <Link href="/docs/v2.0.0/gettingStarted/installation/buildAndRun" className="btn btn-secondary">
                    Setup Guide
                  </Link>
                </div>
              </div>

              <div className="distribution-card">
                <div className="distribution-header">
                  <div className="distribution-icon">📦</div>
                  <h3 className="distribution-title">ZIP Distribution</h3>
                </div>
                <p className="distribution-description">
                  Download pre-built ZIP package with all dependencies included. Perfect for quick evaluation and testing environments.
                </p>
                <div className="distribution-features">
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>All dependencies included</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Ready-to-run scripts</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Sample configurations</span>
                  </div>
                  <div className="feature-item">
                    <div className="feature-icon">✓</div>
                    <span>Quick evaluation</span>
                  </div>
                </div>
                <div className="distribution-cta">
                  <a href="https://github.com/bloxbean/yaci-store/releases/latest" target="_blank" className="btn btn-primary">
                    Download ZIP
                  </a>
                  <Link href="/docs/v2.0.0/gettingStarted/installation/buildAndRun" className="btn btn-secondary">
                    Setup Guide
                  </Link>
                </div>
              </div>

            </div>

          </section>

          {/* Trusted By Section */}
          <section className="trusted-by-section">
            <div className="section-header">
              <h2 className="section-title">Trusted By</h2>
              <p className="section-subtitle">
                <strong>Trusted by the Cardano ecosystem.</strong>
              </p>
            </div>

            <div className="trusted-projects">
              <div className="project-card featured">
                <div className="project-icon">🔄</div>
                <div className="project-info">
                  <h3 className="project-name">Cardano Rosetta Java</h3>
                  <p className="project-description">
                    Official Rosetta API implementation for Cardano by Cardano Foundation, enabling seamless blockchain integration for exchanges and institutional users.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/cardano-foundation/cardano-rosetta-java" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">🗳️</div>
                <div className="project-info">
                  <h3 className="project-name">CF Cardano Ballot</h3>
                  <p className="project-description">
                    Hybrid on- and off-chain voting system for the Cardano Ecosystem by Cardano Foundation, powering governance and community decisions.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/cardano-foundation/cf-cardano-ballot" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">🏦</div>
                <div className="project-info">
                  <h3 className="project-name">CF Reeve Platform</h3>
                  <p className="project-description">
                    Integrates traditional accounting systems with blockchain technology by Cardano Foundation, bridging enterprise finance and DeFi.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/cardano-foundation/cf-reeve-platform" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">🏷️</div>
                <div className="project-info">
                  <h3 className="project-name">CF AdaHandle Resolver</h3>
                  <p className="project-description">
                    Efficient resolution service for AdaHandle by Cardano Foundation, enabling fast and reliable NFT handle lookups.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/cardano-foundation/cf-adahandle-resolver" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">💧</div>
                <div className="project-info">
                  <h3 className="project-name">FluidTokens Aquarium Node</h3>
                  <p className="project-description">
                    Java app that indexes FluidTokens users' Tanks UTXOs and processes Scheduled Transactions for DeFi automation.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/FluidTokens/ft-aquarium-node" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">🍨</div>
                <div className="project-info">
                  <h3 className="project-name">SundaeSwap Analytics</h3>
                  <p className="project-description">
                    Crawler app by Easy1 Staking storing scoop transaction info for DEX analytics and trading insights.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/easy1staking-com/sundaeswap-scooper-analytics" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">🔷</div>
                <div className="project-info">
                  <h3 className="project-name">AdaMatic</h3>
                  <p className="project-description">
                    Professional Cardano staking platform by Easy1 Staking, providing enterprise-grade delegation services and analytics.
                  </p>
                  <div className="project-links">
                    <a href="https://adamatic.xyz/" target="_blank" className="project-link">
                      Visit Site
                    </a>
                  </div>
                </div>
              </div>

              <div className="project-card">
                <div className="project-icon">⚙️</div>
                <div className="project-info">
                  <h3 className="project-name">CBI Backend Service</h3>
                  <p className="project-description">
                    Backend infrastructure service by Cardano Fans (CRFA) for community-driven blockchain applications and tools.
                  </p>
                  <div className="project-links">
                    <a href="https://github.com/Cardano-Fans/cbi-backend" target="_blank" className="project-link">
                      GitHub
                    </a>
                  </div>
                </div>
              </div>
            </div>

          </section>

          {/* What's New in Beta 3 Section */}
          <section className="whats-new-section">
            <div className="section-header">
              <h2 className="section-title">What's New in Beta 3</h2>
              <p className="section-subtitle">
                Discover the latest features and improvements in Yaci Store v2.0.0-beta3
              </p>
            </div>

            <div className="whats-new-grid">
              <div className="feature-card highlight">
                <div className="feature-icon">📊</div>
                <div className="feature-content">
                  <h3 className="feature-title">Enhanced Monitoring</h3>
                  <p className="feature-description">
                    Native Prometheus metrics integration with pre-built Grafana dashboards for comprehensive observability and system health monitoring.
                  </p>
                  <div className="feature-badge">New</div>
                </div>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🌐</div>
                <div className="feature-content">
                  <h3 className="feature-title">Extended Network Coverage</h3>
                  <p className="feature-description">
                    Improved epoch support across all Cardano networks - Mainnet (epoch 569+), Preprod (227+), and Preview (989+).
                  </p>
                  <div className="feature-badge">Improved</div>
                </div>
              </div>

              <div className="feature-card">
                <div className="feature-icon">🗳️</div>
                <div className="feature-content">
                  <h3 className="feature-title">Advanced Governance</h3>
                  <p className="feature-description">
                    Better governance state data management with fixed proposal status calculations and improved DRep distribution accuracy.
                  </p>
                  <div className="feature-badge">Fixed</div>
                </div>
              </div>

              <div className="feature-card">
                <div className="feature-icon">⚡</div>
                <div className="feature-content">
                  <h3 className="feature-title">Performance Optimizations</h3>
                  <p className="feature-description">
                    Enhanced reward calculation efficiency during epoch transitions and improved database management tools.
                  </p>
                  <div className="feature-badge">Enhanced</div>
                </div>
              </div>
            </div>

            <div className="whats-new-footer">
              <div className="release-info">
                <div className="release-meta">
                  <span className="release-version">v2.0.0-beta3</span>
                  <span className="release-date">March 2024</span>
                  <span className="release-status">Current Stable Beta</span>
                </div>
                <div className="release-actions">
                  <Link href="/docs/v2.0.0/changelogs" className="btn btn-outline">
                    View Full Changelog
                  </Link>
                </div>
              </div>
            </div>
          </section>

          {/* Community Section */}
          <section className="community-section">
            <h2 className="community-title">Join the Community</h2>
            <p className="community-desc">
              Connect with developers, get support, and contribute to the future of Cardano infrastructure.
            </p>
            
            <div className="community-links">
              <a href="https://github.com/bloxbean/yaci-store" target="_blank" className="community-link">
                <span className="community-icon">🐙</span>
                GitHub
              </a>
              <a href="https://discord.gg/cardano-developers" target="_blank" className="community-link">
                <span className="community-icon">🎮</span>
                Discord
              </a>
              <a href="/docs/v2.0.0" className="community-link">
                <span className="community-icon">📖</span>
                Documentation
              </a>
              <a href="https://x.com/bloxbean?lang=en" target="_blank" className="community-link">
                <span className="community-icon">𝕏</span>
                X
              </a>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}