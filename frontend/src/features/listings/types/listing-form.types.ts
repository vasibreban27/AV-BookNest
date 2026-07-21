import { z } from 'zod'
import { listingSchema } from '../schemas/listing.schema'

export type ListingFormValues = z.infer<typeof listingSchema>
