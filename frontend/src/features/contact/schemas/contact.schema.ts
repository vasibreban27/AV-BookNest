import { z } from 'zod'
import { CONTACT_TOPICS } from '../types/contact.types'

export const contactSchema = z.object({
  name: z.string().trim().min(2, 'Introdu numele tău.').max(201, 'Numele este prea lung.'),
  email: z.string().trim().email('Introdu o adresă de email validă.').max(254),
  topic: z.enum(CONTACT_TOPICS),
  subject: z.string().trim().min(5, 'Descrie pe scurt subiectul.').max(150, 'Subiectul este prea lung.'),
  message: z.string().trim().min(20, 'Mesajul trebuie să aibă cel puțin 20 de caractere.').max(4000, 'Mesajul poate avea cel mult 4.000 de caractere.'),
  privacyAccepted: z.boolean().refine(Boolean, 'Trebuie să accepți prelucrarea datelor pentru a trimite mesajul.'),
  website: z.string().max(100),
})

export type ContactFormValues = z.infer<typeof contactSchema>
