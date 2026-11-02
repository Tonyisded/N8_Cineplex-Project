-- Match the existing payments INSERT grants without granting table/schema ownership.
DO $$
DECLARE recipient RECORD;
BEGIN
  FOR recipient IN
    SELECT DISTINCT acl.grantee FROM pg_class c
    CROSS JOIN LATERAL aclexplode(COALESCE(c.relacl, acldefault('r', c.relowner))) acl
    WHERE c.oid = 'public.payments'::regclass AND acl.privilege_type = 'INSERT'
  LOOP
    IF recipient.grantee = 0 THEN
      GRANT USAGE ON SEQUENCE public.payos_order_code_seq TO PUBLIC;
    ELSE
      EXECUTE format('GRANT USAGE ON SEQUENCE public.payos_order_code_seq TO %I', pg_get_userbyid(recipient.grantee));
    END IF;
  END LOOP;
END $$;
