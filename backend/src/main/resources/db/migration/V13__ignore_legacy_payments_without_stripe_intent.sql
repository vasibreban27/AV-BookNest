-- Payments created before the Stripe integration have no remote PaymentIntent.
-- Keep their historical state and exclude them from the Stripe expiration scheduler.
UPDATE payments
SET expires_at = NULL
WHERE provider_payment_id IS NULL
  AND expires_at IS NOT NULL;
