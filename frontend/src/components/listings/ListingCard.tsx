import { Link } from 'react-router-dom'
import { BookOutlineIcon, EditIcon } from '../common/icons/AppIcons'
import { formatBookPrice } from '../../features/catalog/utils/catalogFormatters'
import { formatListingDate, formatListingStatus, getListingStatusTone } from '../../features/listings/utils/listingFormatters'
import type { ListingCardProps } from './types/listing-component.types'

export function ListingCard({ book, actionPending, onArchive, onPublish }: ListingCardProps) {
  const canEdit = book.status === 'AVAILABLE' || book.status === 'ARCHIVED' || book.status === 'DRAFT'

  return (
    <article className="listing-card">
      <div className="listing-card__cover">
        {book.coverImageUrl ? <img src={book.coverImageUrl} alt={`Coperta cărții ${book.title}`} /> : <BookOutlineIcon />}
      </div>
      <div className="listing-card__body">
        <div className="listing-card__topline">
          <span className={`listing-status listing-status--${getListingStatusTone(book.status)}`}>{formatListingStatus(book.status)}</span>
          <small>Actualizată {formatListingDate(book.updatedAt)}</small>
        </div>
        <p>{book.category.name}</p>
        <h2>{book.title}</h2>
        <span>de {book.author}</span>
        <strong>{formatBookPrice(book.price)}</strong>
        {(book.status === 'RESERVED' || book.status === 'SOLD') && (
          <div className="listing-card__notice">{book.status === 'RESERVED' ? 'Cartea face parte dintr-o comandă și nu poate fi modificată.' : 'Această carte a fost vândută.'}</div>
        )}
      </div>
      <div className="listing-card__actions">
        {canEdit && <Link to={`/my-books/${book.id}/edit`}><EditIcon />Editează</Link>}
        {book.status === 'AVAILABLE' && <button type="button" disabled={actionPending} onClick={() => onArchive(book.id)}>Arhivează</button>}
        {(book.status === 'ARCHIVED' || book.status === 'DRAFT') && <button className="listing-card__primary" type="button" disabled={actionPending} onClick={() => onPublish(book.id)}>Publică</button>}
      </div>
    </article>
  )
}
