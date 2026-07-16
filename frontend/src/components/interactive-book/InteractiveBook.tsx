import { useRef, useState, type PointerEvent } from 'react'
import { SparkleIcon } from '../common/icons/AppIcons'
import type { InteractiveBookProps } from './types/interactive-book.types'

export function InteractiveBook({ className = '' }: InteractiveBookProps) {
  const [isOpen, setIsOpen] = useState(false)
  const sceneRef = useRef<HTMLDivElement>(null)

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const bounds = event.currentTarget.getBoundingClientRect()
    const horizontal = (event.clientX - bounds.left) / bounds.width - 0.5
    const vertical = (event.clientY - bounds.top) / bounds.height - 0.5

    sceneRef.current?.style.setProperty('--tilt-x', `${vertical * -7}deg`)
    sceneRef.current?.style.setProperty('--tilt-y', `${horizontal * 10}deg`)
    sceneRef.current?.style.setProperty('--light-x', `${(horizontal + 0.5) * 100}%`)
    sceneRef.current?.style.setProperty('--light-y', `${(vertical + 0.5) * 100}%`)
  }

  const resetPointerPosition = () => {
    sceneRef.current?.style.setProperty('--tilt-x', '0deg')
    sceneRef.current?.style.setProperty('--tilt-y', '0deg')
    sceneRef.current?.style.setProperty('--light-x', '50%')
    sceneRef.current?.style.setProperty('--light-y', '35%')
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

      <div className="book-experience__hint" aria-live="polite">
        <SparkleIcon />
        <span>{isOpen ? 'Mai apasă o dată pentru a închide' : 'Mișcă mouse-ul și deschide cartea'}</span>
      </div>
    </div>
  )
}
