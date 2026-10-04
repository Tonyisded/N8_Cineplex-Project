
-- Domain invariants not expressible in Prisma schema.
ALTER TABLE "users" ADD CONSTRAINT "users_credit_nonnegative" CHECK ("credit_balance" >= 0);
ALTER TABLE "movies" ADD CONSTRAINT "movies_duration_positive" CHECK ("duration_minutes" > 0);
ALTER TABLE "auditoriums" ADD CONSTRAINT "auditoriums_capacity_positive" CHECK ("capacity" > 0);
ALTER TABLE "showtimes" ADD CONSTRAINT "showtimes_time_order" CHECK ("end_at" > "start_at"), ADD CONSTRAINT "showtimes_price_nonnegative" CHECK ("base_price" >= 0);
ALTER TABLE "showtime_seats" ADD CONSTRAINT "showtime_seats_price_nonnegative" CHECK ("price" >= 0), ADD CONSTRAINT "showtime_seats_number_positive" CHECK ("seat_number" > 0);
ALTER TABLE "combos" ADD CONSTRAINT "combos_price_nonnegative" CHECK ("price" >= 0);
ALTER TABLE "orders" ADD CONSTRAINT "orders_amounts_nonnegative" CHECK ("subtotal" >= 0 AND "discount" >= 0 AND "credit_used" >= 0 AND "total_amount" >= 0), ADD CONSTRAINT "orders_combo_quantity_nonnegative" CHECK ("combo_quantity" >= 0);
ALTER TABLE "order_seats" ADD CONSTRAINT "order_seats_price_nonnegative" CHECK ("price" >= 0);
ALTER TABLE "payments" ADD CONSTRAINT "payments_amounts_nonnegative" CHECK ("expected_amount" >= 0 AND "received_amount" >= 0);
