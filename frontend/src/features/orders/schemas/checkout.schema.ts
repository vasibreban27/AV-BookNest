import { z } from 'zod'

export const checkoutSchema = z.object({
  easyboxId: z
    .string()
    .trim()
    .min(2, 'Introdu codul Easybox.')
    .max(50, 'Codul Easybox este prea lung.'),
  easyboxName: z
    .string()
    .trim()
    .min(3, 'Introdu numele sau adresa Easyboxului.')
    .max(120, 'Numele Easyboxului este prea lung.'),
})
