import { Link } from 'react-router-dom'
import { BookOutlineIcon } from '../common/icons/AppIcons'
import type { ListingErrorStateProps } from './types/listing-component.types'

export function ListingsLoadingState() {
  return <div className="listings-loading" aria-label="Se încarcă anunțurile">{Array.from({ length: 3 }, (_, index) => <span key={index}><i /><i /><i /></span>)}</div>
}

export function ListingsEmptyState() {
  return <div className="listings-state"><span><BookOutlineIcon /></span><h2>Raftul tău de vânzare este gol.</h2><p>Publică prima carte și ajut-o să ajungă la următorul cititor.</p><Link to="/sell">Vinde o carte</Link></div>
}

export function ListingsErrorState({ onRetry }: ListingErrorStateProps) {
  return <div className="listings-state listings-state--error" role="alert"><span><BookOutlineIcon /></span><h2>Nu am putut încărca anunțurile.</h2><p>Verifică backend-ul și încearcă din nou.</p><button type="button" onClick={onRetry}>Reîncearcă</button></div>
}

export function ListingFormLoadingState() {
  return <div className="listing-form-loading" aria-label="Se încarcă formularul"><span /><span /></div>
}
