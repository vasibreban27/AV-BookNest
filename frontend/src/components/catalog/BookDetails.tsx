import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../features/auth/hooks/useAuth'
import {
  formatBookCondition,
  formatBookPrice,
  getBookCoverTone,
} from '../../features/catalog/utils/catalogFormatters'
import { AddToCartButton } from '../cart/AddToCartButton'
import { BookOutlineIcon, EditIcon } from '../common/icons/AppIcons'
import { FavoriteButton } from '../wishlist/FavoriteButton'
import type { BookDetailsProps } from './types/catalog-component.types'

export function BookDetails({ book }: BookDetailsProps) {
  const { user } = useAuth()
  const [coverFailed, setCoverFailed] = useState(false)
  const showCoverImage = Boolean(book.coverImageUrl) && !coverFailed
  const isOwnBook = user?.id === book.sellerId
  const facts = [
    ['Limbă', book.language],
    ['Editură', book.publisher],
    ['An publicare', book.publishedYear?.toString()],
    ['ISBN', book.isbn],
  ].filter((fact): fact is [string, string] => Boolean(fact[1]))

  return (
    <section className="book-details">
      <div
        className={`book-details__cover book-details__cover--${getBookCoverTone(book.id)}`}
      >
        {showCoverImage ? (
          <img
            src={book.coverImageUrl ?? ''}
            alt={`Coperta cărții ${book.title}`}
            onError={() => setCoverFailed(true)}
          />
        ) : (
          <div className="book-details__cover-fallback">
            <BookOutlineIcon />
            <strong>{book.title}</strong>
            <span>{book.author}</span>
          </div>
        )}
        <span className="book-details__condition">
          {formatBookCondition(book.bookCondition)}
        </span>
      </div>

      <article className="book-details__content">
        <span className="home-kicker">{book.category.name}</span>
        <h1>{book.title}</h1>
        <p className="book-details__author">de {book.author}</p>

        <div className="book-details__divider" />

        <section className="book-details__description" aria-labelledby="book-description-title">
          <h2 id="book-description-title">Despre carte</h2>
          <p>
            {book.description ||
              'Vânzătorul nu a adăugat încă o descriere pentru această carte.'}
          </p>
        </section>

        <section className="book-details__facts" aria-labelledby="book-facts-title">
          <h2 id="book-facts-title">Detalii bibliografice</h2>
          {facts.length > 0 ? (
            <dl>
              {facts.map(([label, value]) => (
                <div key={label}>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          ) : (
            <p>Nu sunt disponibile alte detalii bibliografice.</p>
          )}
        </section>
      </article>

      <aside className="book-details__purchase" aria-label="Opțiuni de cumpărare">
        <span className="book-details__purchase-label">Preț</span>
        <strong className="book-details__price">{formatBookPrice(book.price)}</strong>
        <div className="book-details__seller">
          <span>{book.sellerName.charAt(0).toUpperCase()}</span>
          <div>
            <small>Vândută de</small>
            <strong>{book.sellerName}</strong>
          </div>
        </div>

        {isOwnBook ? (
          <Link className="book-details__manage" to={`/my-books/${book.id}/edit`}>
            <EditIcon /> Gestionează anunțul
          </Link>
        ) : (
          <AddToCartButton bookId={book.id} sellerId={book.sellerId} />
        )}

        <div className="book-details__favorite">
          <FavoriteButton bookId={book.id} />
          <span>Salvează în favorite</span>
        </div>

        <p className="book-details__assurance">
          Cartea este rezervată numai după finalizarea comenzii.
        </p>
      </aside>
    </section>
  )
}
