ALTER TABLE orders ADD COLUMN voucher_code TEXT;
CREATE INDEX orders_user_voucher_idx ON orders(user_id, voucher_code, status);
