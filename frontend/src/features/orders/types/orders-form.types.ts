import { z } from 'zod'
import { checkoutSchema } from '../schemas/checkout.schema'

export type CheckoutFormValues = z.infer<typeof checkoutSchema>
