import { useEffect, useRef, useState, type PointerEvent } from 'react'
import type { InteractiveBookProps } from './types/interactive-book.types'

type MotionState = {
  tiltX: number
  tiltY: number
  lightX: number
  lightY: number
}

export function InteractiveBook({ className = '' }: InteractiveBookProps) {
  const [isOpen, setIsOpen] = useState(false)
  const sceneRef = useRef<HTMLDivElement>(null)
  const frameRef = useRef<number | null>(null)
  const reducedMotionRef = useRef(false)
  const targetRef = useRef<MotionState>({ tiltX: 0, tiltY: 0, lightX: 50, lightY: 35 })
  const currentRef = useRef<MotionState>({ tiltX: 0, tiltY: 0, lightX: 50, lightY: 35 })

  const animateTowardsPointer = () => {
    const current = currentRef.current
    const target = targetRef.current
    const ease = 0.12

    current.tiltX += (target.tiltX - current.tiltX) * ease
    current.tiltY += (target.tiltY - current.tiltY) * ease
    current.lightX += (target.lightX - current.lightX) * ease
    current.lightY += (target.lightY - current.lightY) * ease

    sceneRef.current?.style.setProperty('--tilt-x', `${current.tiltX.toFixed(3)}deg`)
    sceneRef.current?.style.setProperty('--tilt-y', `${current.tiltY.toFixed(3)}deg`)
    sceneRef.current?.style.setProperty('--light-x', `${current.lightX.toFixed(2)}%`)
    sceneRef.current?.style.setProperty('--light-y', `${current.lightY.toFixed(2)}%`)

    const distance =
      Math.abs(target.tiltX - current.tiltX) +
      Math.abs(target.tiltY - current.tiltY) +
      Math.abs(target.lightX - current.lightX) +
      Math.abs(target.lightY - current.lightY)

    if (distance > 0.05) frameRef.current = requestAnimationFrame(animateTowardsPointer)
    else frameRef.current = null
  }

  const scheduleMotion = () => {
    if (frameRef.current === null) {
      frameRef.current = requestAnimationFrame(animateTowardsPointer)
    }
  }

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    const syncPreference = () => {
      reducedMotionRef.current = mediaQuery.matches
    }
    syncPreference()
    mediaQuery.addEventListener('change', syncPreference)

    return () => {
      mediaQuery.removeEventListener('change', syncPreference)
      if (frameRef.current !== null) cancelAnimationFrame(frameRef.current)
    }
  }, [])

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (event.pointerType === 'touch' || reducedMotionRef.current) return
    const bounds = event.currentTarget.getBoundingClientRect()
    const horizontal = (event.clientX - bounds.left) / bounds.width - 0.5
    const vertical = (event.clientY - bounds.top) / bounds.height - 0.5

    targetRef.current = {
      tiltX: vertical * -5,
      tiltY: horizontal * 7,
      lightX: (horizontal + 0.5) * 100,
      lightY: (vertical + 0.5) * 100,
    }
    scheduleMotion()
  }

  const resetPointerPosition = () => {
    targetRef.current = { tiltX: 0, tiltY: 0, lightX: 50, lightY: 35 }
    scheduleMotion()
  }

  return (
    <div
      className={`book-experience ${className}`}
      ref={sceneRef}
      onPointerMove={handlePointerMove}
      onPointerLeave={resetPointerPosition}
    >
      <span className="book-experience__halo" aria-hidden="true" />
      <span className="book-experience__particle book-experience__particle--one" aria-hidden="true">✦</span>
      <span className="book-experience__particle book-experience__particle--two" aria-hidden="true">·</span>
      <span className="book-experience__particle book-experience__particle--three" aria-hidden="true">✧</span>

      <div className="book-experience__stage">
        <button
          className={`story-book${isOpen ? ' story-book--open' : ''}`}
          type="button"
          onClick={() => setIsOpen((value) => !value)}
          aria-pressed={isOpen}
          aria-label={isOpen ? 'Închide cartea' : 'Deschide cartea'}
        >
          <span className="story-book__back-cover" aria-hidden="true" />
          <span className="story-book__paper-stack" aria-hidden="true">
            <i /><i /><i /><i /><i />
          </span>

          <span className="story-book__base-page" aria-hidden="true">
            <span className="story-book__chapter">Capitolul următor</span>
            <strong>O carte bună<br />își găsește mereu<br />un nou cititor.</strong>
            <span className="story-book__ornament">✦</span>
            <small>BookNest</small>
          </span>

          <span className="story-book__sheet story-book__sheet--three" aria-hidden="true">
            <span>Unele povești<br />merită recitite.</span>
          </span>
          <span className="story-book__sheet story-book__sheet--two" aria-hidden="true">
            <span>Dintr-o bibliotecă<br />în alta.</span>
          </span>
          <span className="story-book__sheet story-book__sheet--one" aria-hidden="true">
            <span>Întoarce pagina.</span>
          </span>

          <span className="story-book__front-cover" aria-hidden="true">
            <span className="story-book__cover-face story-book__cover-face--front">
              <span className="story-book__cover-frame">
                <small>BookNest</small>
                <i>O colecție de</i>
                <strong>Povești<br />în mișcare</strong>
                <span className="story-book__cover-mark">BN</span>
                <em>deschide pentru a explora</em>
              </span>
            </span>
            <span className="story-book__cover-face story-book__cover-face--inside">
              <span className="story-book__inside-quote">„Cărțile ne găsesc exact când avem nevoie de ele.”</span>
              <span className="story-book__inside-lines" />
            </span>
          </span>

          <span className="story-book__bookmark" aria-hidden="true" />
          <span className="story-book__spine" aria-hidden="true" />
        </button>
      </div>
    </div>
  )
}
