import { Link } from 'react-router-dom'
import { BookOutlineIcon } from '../common/icons/AppIcons'

export function BookDetailsLoadingState() {
  return (
    <div className="book-details-loading" aria-label="Se încarcă detaliile cărții">
      <span />
      <div><i /><i /><i /><i /></div>
      <aside><i /><i /><i /></aside>
    </div>
  )
}

type BookDetailsErrorStateProps = {
  onRetry: () => void
}

export function BookDetailsErrorState({ onRetry }: BookDetailsErrorStateProps) {
  return (
    <div className="book-details-state" role="alert">
      <span><BookOutlineIcon /></span>
      <h1>Cartea nu a putut fi încărcată.</h1>
      <p>Este posibil ca anunțul să nu mai fie disponibil.</p>
      <div>
        <button type="button" onClick={onRetry}>Reîncearcă</button>
        <Link to={{ pathname: '/', hash: '#catalog' }}>Înapoi la catalog</Link>
      </div>
    </div>
  )
}
