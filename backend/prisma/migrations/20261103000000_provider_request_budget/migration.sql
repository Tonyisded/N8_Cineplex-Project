CREATE TABLE provider_api_governor (
  channel_key TEXT PRIMARY KEY,
  next_allowed_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  blocked_until TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lease_until TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lease_token UUID,
  streaks JSONB NOT NULL DEFAULT '{}'
);
CREATE TABLE provider_webhook_inbox (
  event_key TEXT PRIMARY KEY,
  order_code BIGINT NOT NULL,
  reference TEXT NOT NULL,
  payment_link_id TEXT NOT NULL,
  next_attempt_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lease_until TIMESTAMPTZ(3),
  completed_at TIMESTAMPTZ(3),
  received_at TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX provider_inbox_due_idx ON provider_webhook_inbox(next_attempt_at) WHERE completed_at IS NULL;
DO $$ DECLARE recipient RECORD; BEGIN
  FOR recipient IN SELECT DISTINCT acl.grantee FROM pg_class c
    CROSS JOIN LATERAL aclexplode(COALESCE(c.relacl,acldefault('r',c.relowner))) acl
    WHERE c.oid='public.payments'::regclass AND acl.privilege_type='INSERT'
  LOOP
    IF recipient.grantee <> 0 THEN
      EXECUTE format('GRANT SELECT,INSERT,UPDATE ON public.provider_api_governor,public.provider_webhook_inbox TO %I',pg_get_userbyid(recipient.grantee));
    END IF;
  END LOOP;
END $$;
