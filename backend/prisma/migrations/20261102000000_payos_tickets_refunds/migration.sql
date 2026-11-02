-- User-approved removal of unused N8 Credit; run only after backup/preflight.
-- Never edit the applied bootstrap migration.
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM orders WHERE credit_used > 0 AND status IN ('PAID','REFUNDED')) THEN
    RAISE EXCEPTION 'Paid legacy Credit requires review before removal';
  END IF;
  IF EXISTS (SELECT 1 FROM orders WHERE credit_used > 0 AND status='PENDING_PAYMENT') THEN
    RAISE EXCEPTION 'Drain/cancel legacy Credit holds before migration';
  END IF;
END $$;
ALTER TABLE users DROP CONSTRAINT users_credit_nonnegative, DROP COLUMN credit_balance;
ALTER TABLE orders DROP CONSTRAINT orders_amounts_nonnegative, DROP COLUMN credit_used;
ALTER TABLE orders ADD CONSTRAINT orders_amounts_nonnegative CHECK(subtotal>=0 AND discount>=0 AND total_amount>=0);
ALTER TYPE "OrderStatus" ADD VALUE 'REFUND_PENDING';
ALTER TYPE "TicketStatus" ADD VALUE 'REFUND_PENDING';
CREATE TYPE "PaymentKind" AS ENUM ('INTENT','RECEIPT','REFUND');
CREATE SEQUENCE payos_order_code_seq START 1000000 MAXVALUE 9007199254740991 NO CYCLE;
ALTER TABLE orders ADD COLUMN checkout_revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN checkout_confirmed_at TIMESTAMPTZ(3);
ALTER TABLE payments ALTER COLUMN order_id DROP NOT NULL,
 ADD COLUMN kind "PaymentKind" NOT NULL DEFAULT 'INTENT',
 ADD COLUMN intent_order_id UUID UNIQUE,
 ADD COLUMN refund_order_id UUID UNIQUE,
 ADD COLUMN parent_id UUID REFERENCES payments(id) ON DELETE RESTRICT,
 ADD COLUMN provider_order_code BIGINT UNIQUE,
 ADD COLUMN provider_object_id TEXT UNIQUE,
 ADD COLUMN operation_state TEXT NOT NULL DEFAULT 'REQUESTED',
 ADD COLUMN recipient_cipher TEXT,
 ADD COLUMN snapshot JSONB NOT NULL DEFAULT '{}',
 ADD COLUMN notification_state JSONB NOT NULL DEFAULT '{}',
 ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
 ADD COLUMN next_attempt_at TIMESTAMPTZ(3),
 ADD COLUMN lease_until TIMESTAMPTZ(3),
 ADD COLUMN updated_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 ADD COLUMN resolved_by UUID,
 ADD COLUMN resolution_note TEXT;
CREATE INDEX payments_work_idx ON payments(kind,operation_state,next_attempt_at);
ALTER TABLE tickets ADD COLUMN snapshot JSONB NOT NULL DEFAULT '{}',
 ADD COLUMN email_attempts INTEGER NOT NULL DEFAULT 0,
 ADD COLUMN email_next_attempt_at TIMESTAMPTZ(3),
 ADD COLUMN email_lease_until TIMESTAMPTZ(3),
 ADD COLUMN fcm_sent_at TIMESTAMPTZ(3);
