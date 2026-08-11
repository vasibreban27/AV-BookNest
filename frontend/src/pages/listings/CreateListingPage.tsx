import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ListingForm } from '../../components/listings/ListingForm'
import { ListingFormLoadingState, ListingsErrorState } from '../../components/listings/ListingStates'
import { useCreateListing, useListingCategories } from '../../features/listings/hooks/useListings'
import { getListingErrorMessage, ListingCreatedWithoutCoverError } from '../../features/listings/utils/listingErrors'
import type { ListingFormSubmission } from '../../components/listings/types/listing-component.types'
import { useStripeConnectStatus } from '../../features/payments/hooks/useStripeConnect'

export function CreateListingPage() {
  const navigate = useNavigate()
  const categoriesQuery = useListingCategories()
  const createListing = useCreateListing()
  const stripeStatus = useStripeConnectStatus()
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
          {(categoriesQuery.isLoading || stripeStatus.isLoading) && <ListingFormLoadingState />}
          {categoriesQuery.isError && <ListingsErrorState onRetry={() => void categoriesQuery.refetch()} />}
          {!stripeStatus.isLoading && !stripeStatus.data?.payoutsEnabled && (
            <div className="listings-state">
              <h2>Conectează Stripe înainte să publici</h2>
              <p>Finalizează onboardingul sandbox pentru ca viitorii cumpărători să poată plăti fără blocaje la checkout.</p>
              <Link className="listing-state-action" to="/account">Configurează Stripe în Contul meu</Link>
            </div>
          )}
          {stripeStatus.data?.payoutsEnabled && categoriesQuery.data && categoriesQuery.data.length > 0 && (
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
