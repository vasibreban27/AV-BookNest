import { z } from 'zod'

const currentYear = new Date().getFullYear()

export const listingSchema = z.object({
  title: z.string().trim().min(2, 'Introdu titlul cărții.').max(255, 'Titlul este prea lung.'),
  author: z.string().trim().min(2, 'Introdu autorul cărții.').max(255, 'Numele autorului este prea lung.'),
  isbn: z.string().trim().max(20, 'ISBN-ul este prea lung.'),
  description: z.string().trim().max(4000, 'Descrierea este prea lungă.'),
  price: z.number({ error: 'Introdu un preț valid.' }).min(0, 'Prețul nu poate fi negativ.').max(999999, 'Prețul este prea mare.'),
  bookCondition: z.enum(['NEW', 'LIKE_NEW', 'VERY_GOOD', 'GOOD', 'ACCEPTABLE']),
  language: z.string().trim().min(2, 'Introdu limba cărții.').max(100, 'Denumirea limbii este prea lungă.'),
  publisher: z.string().trim().max(255, 'Numele editurii este prea lung.'),
  publishedYear: z.number({ error: 'Introdu un an valid.' }).int().min(1000, 'Anul trebuie să aibă patru cifre.').max(currentYear, 'Anul nu poate fi în viitor.').optional(),
  weightGrams: z.number({ error: 'Introdu greutatea.' }).int().min(1, 'Greutatea trebuie sa fie pozitiva.').max(19850, 'Greutatea maxima este 19,85 kg.'),
  lengthMm: z.number({ error: 'Introdu lungimea.' }).int().min(10, 'Lungimea minima este 10 mm.').max(450, 'Lungimea maxima este 450 mm.'),
  widthMm: z.number({ error: 'Introdu latimea.' }).int().min(10, 'Latimea minima este 10 mm.').max(425, 'Latimea maxima este 425 mm.'),
  heightMm: z.number({ error: 'Introdu grosimea.' }).int().min(1, 'Grosimea trebuie sa fie pozitiva.').max(370, 'Grosimea maxima este 370 mm.'),
  categoryId: z.number({ error: 'Alege o categorie.' }).int().positive('Alege o categorie.'),
})
