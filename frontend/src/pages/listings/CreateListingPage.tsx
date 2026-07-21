import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ListingForm } from '../../components/listings/ListingForm'
import { ListingFormLoadingState, ListingsErrorState } from '../../components/listings/ListingStates'
import { useCreateListing, useListingCategories } from '../../features/listings/hooks/useListings'
import { getListingErrorMessage, ListingCreatedWithoutCoverError } from '../../features/listings/utils/listingErrors'
import type { ListingFormSubmission } from '../../components/listings/types/listing-component.types'

export function CreateListingPage() {
  const navigate = useNavigate()
  const categoriesQuery = useListingCategories()
  const createListing = useCreateListing()
  const [submitError, setSubmitError] = useState<string | null>(null)

  const handleSubmit = async ({ payload, coverFile }: ListingFormSubmission) => {
    setSubmitError(null)
    try {
      await createListing.mutateAsync({ payload, coverFile })
      navigate('/my-books', { replace: true, state: { created: true } })
    } catch (error) {
      if (error instanceof ListingCreatedWithoutCoverError) {
        navigate(`/my-books/${error.listing.id}/edit`, {
          replace: true,
          state: { coverUploadFailed: true },
        })
        return
      }
      setSubmitError(getListingErrorMessage(error))
    }
  }

  return (
    <main className="listing-editor-page">
      <section className="listing-editor-content">
        <div className="listings-page__container">
          {categoriesQuery.isLoading && <ListingFormLoadingState />}
          {categoriesQuery.isError && <ListingsErrorState onRetry={() => void categoriesQuery.refetch()} />}
          {categoriesQuery.data && categoriesQuery.data.length > 0 && (
            <ListingForm
              categories={categoriesQuery.data}
              mode="create"
              submitError={submitError}
              isPending={createListing.isPending}
              onSubmit={handleSubmit}
            />
          )}
          {categoriesQuery.data?.length === 0 && (
            <div className="listings-state"><h2>Nu există categorii disponibile.</h2><p>Un administrator trebuie să creeze cel puțin o categorie înainte de publicarea cărților.</p></div>
          )}
        </div>
      </section>
    </main>
  )
}
