import { useState } from 'react'
import { formatBookCondition, formatBookPrice, getBookCoverTone } from '../../features/catalog/utils/catalogFormatters'
import { BookOutlineIcon, TrashIcon } from '../common/icons/AppIcons'
import type { CartItemCardProps } from './types/cart-component.types'

export function CartItemCard({ item, isRemoving, onRemove }: CartItemCardProps) {
  const [coverFailed, setCoverFailed] = useState(false)
  const { book } = item
  const showCoverImage = Boolean(book.coverImageUrl) && !coverFailed

  return (
    <article className="cart-item">
      <div className={`cart-item__cover cart-item__cover--${getBookCoverTone(book.id)}`}>
        {showCoverImage ? (
          <img
            src={book.coverImageUrl ?? ''}
            alt={`Coperta cărții ${book.title}`}
            onError={() => setCoverFailed(true)}
          />
        ) : (
          <BookOutlineIcon />
        )}
      </div>

      <div className="cart-item__content">
        <span className="cart-item__category">{book.category.name}</span>
        <h2>{book.title}</h2>
        <p>de {book.author}</p>
        <div className="cart-item__details">
          <span>{formatBookCondition(book.bookCondition)}</span>
          <span>Vânzător: {book.sellerName}</span>
        </div>
      </div>

      <div className="cart-item__actions">
        <strong>{formatBookPrice(book.price)}</strong>
        <button
          type="button"
          onClick={() => onRemove(book.id)}
          disabled={isRemoving}
          aria-label={`Elimină ${book.title} din coș`}
        >
          <TrashIcon />
          <span>{isRemoving ? 'Se elimină...' : 'Elimină'}</span>
        </button>
      </div>
    </article>
  )
}
