import { z } from 'zod'

export const checkoutSchema = z.object({
  easyboxId: z.string().trim().min(1, 'Selectează un Easybox din lista Sameday.'),
  easyboxName: z.string().trim().min(1),
  easyboxAddress: z.string().trim().min(1),
  easyboxCity: z.string().trim().min(1),
  easyboxCounty: z.string().trim().min(1),
  easyboxPostalCode: z.string().trim(),
  recipientName: z.string().trim().min(3, 'Introdu numele destinatarului.').max(200),
  recipientEmail: z.email('Introdu o adresă de email validă.').max(255),
  recipientPhone: z
    .string()
    .trim()
    .regex(/^\+?[0-9 ]{9,20}$/, 'Introdu un număr de telefon valid.'),
})
