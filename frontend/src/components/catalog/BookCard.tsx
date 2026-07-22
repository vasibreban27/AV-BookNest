import { useState } from 'react'
import { Link } from 'react-router-dom'
import { AddToCartButton } from '../cart/AddToCartButton'
import { BookOutlineIcon } from '../common/icons/AppIcons'
import { FavoriteButton } from '../wishlist/FavoriteButton'
import {
  formatBookCondition,
  formatBookPrice,
  getBookCoverTone,
} from '../../features/catalog/utils/catalogFormatters'
import type { BookCardProps } from './types/catalog-component.types'

export function BookCard({ book }: BookCardProps) {
  const [coverFailed, setCoverFailed] = useState(false)
  const showCoverImage = Boolean(book.coverImageUrl) && !coverFailed

  return (
    <article className="book-card">
      <div className={`book-card__cover book-card__cover--${getBookCoverTone(book.id)}`}>
        <FavoriteButton bookId={book.id} />
        <Link
          className="book-card__cover-link"
          to={`/books/${book.id}`}
          aria-label={`Vezi detalii despre ${book.title}`}
        >
          {showCoverImage ? (
            <img
              src={book.coverImageUrl ?? ''}
              alt={`Coperta cărții ${book.title}`}
              onError={() => setCoverFailed(true)}
            />
          ) : (
            <div className="book-card__fallback">
              <BookOutlineIcon />
              <strong>{book.title}</strong>
              <span>{book.author}</span>
            </div>
          )}
        </Link>
        <span className="book-card__condition">
          {formatBookCondition(book.bookCondition)}
        </span>
      </div>

      <div className="book-card__content">
        <p>{book.category.name}</p>
        <h3><Link to={`/books/${book.id}`}>{book.title}</Link></h3>
        <span className="book-card__author">de {book.author}</span>
        <div className="book-card__meta">
          <strong>{formatBookPrice(book.price)}</strong>
          <small>de la {book.sellerName}</small>
        </div>
        <AddToCartButton bookId={book.id} sellerId={book.sellerId} />
      </div>
    </article>
  )
}
