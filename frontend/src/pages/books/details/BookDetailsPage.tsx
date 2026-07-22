import { Link, Navigate, useParams } from 'react-router-dom'
import { BookDetails } from '../../../components/catalog/BookDetails'
import {
  BookDetailsErrorState,
  BookDetailsLoadingState,
} from '../../../components/catalog/BookDetailsStates'
import { useBook } from '../../../features/catalog/hooks/useBook'

export function BookDetailsPage() {
  const { bookId } = useParams()
  const numericBookId = Number(bookId)
  const bookQuery = useBook(numericBookId)

  if (!Number.isInteger(numericBookId) || numericBookId <= 0) {
    return <Navigate to={{ pathname: '/', hash: '#catalog' }} replace />
  }

  return (
    <main className="book-details-page">
      <div className="book-details-page__container">
        <nav className="book-details-page__breadcrumb" aria-label="Navigare contextuală">
          <Link to={{ pathname: '/', hash: '#catalog' }}>Catalog</Link>
          <span aria-hidden="true">/</span>
          <span>{bookQuery.data?.title ?? 'Detalii carte'}</span>
        </nav>

        {bookQuery.isLoading && <BookDetailsLoadingState />}
        {bookQuery.isError && (
          <BookDetailsErrorState onRetry={() => void bookQuery.refetch()} />
        )}
        {bookQuery.data && <BookDetails book={bookQuery.data} key={bookQuery.data.id} />}
      </div>
    </main>
  )
}
