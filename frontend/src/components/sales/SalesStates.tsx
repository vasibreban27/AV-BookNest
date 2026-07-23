import { Link } from 'react-router-dom'
import { PackageIcon } from '../common/icons/AppIcons'

export function SalesLoadingState() {
  return <div className="seller-shipments-loading" aria-label="Se încarcă vânzările"><span><i /><i /><i /></span></div>
}

export function SalesEmptyState() {
  return (
    <div className="seller-shipments-state">
      <span><PackageIcon /></span>
      <h2>Nu ai încă vânzări de procesat.</h2>
      <p>Când cineva comandă una dintre cărțile tale, subcomanda va apărea aici.</p>
      <Link to="/my-books">Vezi cărțile mele</Link>
    </div>
  )
}

export function SalesErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="seller-shipments-state seller-shipments-state--error" role="alert">
      <span><PackageIcon /></span>
      <h2>Nu am putut încărca vânzările.</h2>
      <button type="button" onClick={onRetry}>Reîncearcă</button>
    </div>
  )
}
