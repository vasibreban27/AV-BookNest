import { Link } from 'react-router-dom'
import type { LogoProps } from './types/common-component.types'

export function Logo({ compact = false }: LogoProps) {
  return (
    <Link className="logo" to="/" aria-label="BookNest - pagina principală">
      <svg className="logo__mark" viewBox="0 0 42 42" aria-hidden="true">
        <path d="M8 8.5h8.6A6.4 6.4 0 0 1 23 14.9v19.6h-8.6A6.4 6.4 0 0 1 8 28.1V8.5Z" />
        <path d="M34 8.5h-4.6A6.4 6.4 0 0 0 23 14.9v19.6h4.6a6.4 6.4 0 0 0 6.4-6.4V8.5Z" />
        <path d="M13 14h3.5a3 3 0 0 1 3 3v10.5H16a3 3 0 0 1-3-3V14Z" />
      </svg>
      {!compact && <span>BookNest</span>}
    </Link>
  )
}
