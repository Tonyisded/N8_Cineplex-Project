BEGIN;

-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateEnum
CREATE TYPE "Role" AS ENUM ('CUSTOMER', 'STAFF', 'ADMIN');

-- CreateEnum
CREATE TYPE "AuditoriumType" AS ENUM ('STANDARD', 'IMAX', 'SCREENX', 'GOLD');

-- CreateEnum
CREATE TYPE "AuditoriumStatus" AS ENUM ('ACTIVE', 'INACTIVE');

-- CreateEnum
CREATE TYPE "ShowtimeFormat" AS ENUM ('STANDARD_2D', 'STANDARD_3D', 'IMAX_2D', 'IMAX_3D', 'SCREENX', 'GOLD_CLASS');

-- CreateEnum
CREATE TYPE "ShowtimeStatus" AS ENUM ('ACTIVE', 'CANCELED');

-- CreateEnum
CREATE TYPE "SeatType" AS ENUM ('NORMAL', 'VIP', 'COUPLE', 'GOLD');

-- CreateEnum
CREATE TYPE "SeatStatus" AS ENUM ('AVAILABLE', 'HELD', 'SOLD', 'BLOCKED');

-- CreateEnum
CREATE TYPE "ComboStatus" AS ENUM ('AVAILABLE', 'UNAVAILABLE');

-- CreateEnum
CREATE TYPE "OrderStatus" AS ENUM ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'CANCELED', 'REFUNDED');

-- CreateEnum
CREATE TYPE "OrderSeatStatus" AS ENUM ('HELD', 'ACTIVE', 'CANCELED', 'REFUNDED');

-- CreateEnum
CREATE TYPE "PaymentStatus" AS ENUM ('PENDING', 'PAID', 'EXPIRED', 'NEEDS_ADMIN', 'RESOLVED');

-- CreateEnum
CREATE TYPE "PaymentIssueReason" AS ENUM ('WRONG_AMOUNT', 'WRONG_REFERENCE', 'LATE_PAYMENT');

-- CreateEnum
CREATE TYPE "TicketStatus" AS ENUM ('VALID', 'USED', 'REFUNDED', 'CANCELLED', 'EXPIRED');

-- CreateTable
CREATE TABLE "users" (
    "id" UUID NOT NULL,
    "email" TEXT NOT NULL,
    "password_hash" TEXT,
    "google_id" TEXT,
    "full_name" TEXT NOT NULL,
    "avatar_url" TEXT,
    "role" "Role" NOT NULL DEFAULT 'CUSTOMER',
    "is_active" BOOLEAN NOT NULL DEFAULT true,
    "email_verified_at" TIMESTAMPTZ(3),
    "credit_balance" DECIMAL(14,0) NOT NULL DEFAULT 0,
    "refresh_token_hash" TEXT,
    "fcm_token" TEXT,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "movies" (
    "id" UUID NOT NULL,
    "title" TEXT NOT NULL,
    "poster_url" TEXT,
    "duration_minutes" INTEGER NOT NULL,
    "synopsis" TEXT,
    "age_rating" TEXT NOT NULL,
    "genre" TEXT,
    "language" TEXT,
    "source_url" TEXT NOT NULL,
    "updated_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "movies_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "auditoriums" (
    "id" UUID NOT NULL,
    "name" TEXT NOT NULL,
    "type" "AuditoriumType" NOT NULL,
    "capacity" INTEGER NOT NULL,
    "is_extra" BOOLEAN NOT NULL DEFAULT false,
    "status" "AuditoriumStatus" NOT NULL DEFAULT 'ACTIVE',
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "auditoriums_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "showtimes" (
    "id" UUID NOT NULL,
    "movie_id" UUID NOT NULL,
    "auditorium_id" UUID NOT NULL,
    "start_at" TIMESTAMPTZ(3) NOT NULL,
    "end_at" TIMESTAMPTZ(3) NOT NULL,
    "format" "ShowtimeFormat" NOT NULL,
    "base_price" DECIMAL(14,0) NOT NULL,
    "status" "ShowtimeStatus" NOT NULL DEFAULT 'ACTIVE',
    "source_key" TEXT NOT NULL,
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "showtimes_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "showtime_seats" (
    "id" UUID NOT NULL,
    "showtime_id" UUID NOT NULL,
    "seat_code" TEXT NOT NULL,
    "row_name" TEXT NOT NULL,
    "seat_number" INTEGER NOT NULL,
    "seat_type" "SeatType" NOT NULL,
    "price" DECIMAL(14,0) NOT NULL,
    "status" "SeatStatus" NOT NULL DEFAULT 'AVAILABLE',
    "held_by_user_id" UUID,
    "hold_expires_at" TIMESTAMPTZ(3),
    "updated_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "showtime_seats_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "combos" (
    "id" UUID NOT NULL,
    "name" TEXT NOT NULL,
    "description" TEXT NOT NULL,
    "price" DECIMAL(14,0) NOT NULL,
    "image_url" TEXT,
    "status" "ComboStatus" NOT NULL DEFAULT 'AVAILABLE',
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "combos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "orders" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "showtime_id" UUID NOT NULL,
    "combo_id" UUID,
    "combo_quantity" INTEGER NOT NULL DEFAULT 0,
    "subtotal" DECIMAL(14,0) NOT NULL,
    "discount" DECIMAL(14,0) NOT NULL DEFAULT 0,
    "credit_used" DECIMAL(14,0) NOT NULL DEFAULT 0,
    "total_amount" DECIMAL(14,0) NOT NULL,
    "status" "OrderStatus" NOT NULL DEFAULT 'PENDING_PAYMENT',
    "expires_at" TIMESTAMPTZ(3) NOT NULL,
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "orders_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "order_seats" (
    "id" UUID NOT NULL,
    "order_id" UUID NOT NULL,
    "showtime_seat_id" UUID NOT NULL,
    "seat_code" TEXT NOT NULL,
    "price" DECIMAL(14,0) NOT NULL,
    "status" "OrderSeatStatus" NOT NULL DEFAULT 'HELD',
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "order_seats_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "payments" (
    "id" UUID NOT NULL,
    "order_id" UUID NOT NULL,
    "payment_code" TEXT NOT NULL,
    "expected_amount" DECIMAL(14,0) NOT NULL,
    "received_amount" DECIMAL(14,0),
    "provider_transaction_id" TEXT,
    "transfer_content" TEXT,
    "status" "PaymentStatus" NOT NULL DEFAULT 'PENDING',
    "issue_reason" "PaymentIssueReason",
    "expires_at" TIMESTAMPTZ(3) NOT NULL,
    "paid_at" TIMESTAMPTZ(3),
    "created_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "payments_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "tickets" (
    "id" UUID NOT NULL,
    "order_id" UUID NOT NULL,
    "booking_code" TEXT NOT NULL,
    "qr_token_hash" TEXT NOT NULL,
    "status" "TicketStatus" NOT NULL DEFAULT 'VALID',
    "issued_at" TIMESTAMPTZ(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "checked_in_at" TIMESTAMPTZ(3),
    "combo_redeemed_at" TIMESTAMPTZ(3),
    "refunded_at" TIMESTAMPTZ(3),
    "cancelled_at" TIMESTAMPTZ(3),
    "email_sent_at" TIMESTAMPTZ(3),
    "expires_at" TIMESTAMPTZ(3) NOT NULL,

    CONSTRAINT "tickets_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_email_key" ON "users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "users_google_id_key" ON "users"("google_id");

-- CreateIndex
CREATE UNIQUE INDEX "showtimes_source_key_key" ON "showtimes"("source_key");

-- CreateIndex
CREATE UNIQUE INDEX "showtime_seats_showtime_id_seat_code_key" ON "showtime_seats"("showtime_id", "seat_code");

-- CreateIndex
CREATE UNIQUE INDEX "payments_payment_code_key" ON "payments"("payment_code");

-- CreateIndex
CREATE UNIQUE INDEX "payments_provider_transaction_id_key" ON "payments"("provider_transaction_id");

-- CreateIndex
CREATE UNIQUE INDEX "tickets_order_id_key" ON "tickets"("order_id");

-- CreateIndex
CREATE UNIQUE INDEX "tickets_booking_code_key" ON "tickets"("booking_code");

-- CreateIndex
CREATE UNIQUE INDEX "tickets_qr_token_hash_key" ON "tickets"("qr_token_hash");

-- AddForeignKey
ALTER TABLE "showtimes" ADD CONSTRAINT "showtimes_movie_id_fkey" FOREIGN KEY ("movie_id") REFERENCES "movies"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "showtimes" ADD CONSTRAINT "showtimes_auditorium_id_fkey" FOREIGN KEY ("auditorium_id") REFERENCES "auditoriums"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "showtime_seats" ADD CONSTRAINT "showtime_seats_showtime_id_fkey" FOREIGN KEY ("showtime_id") REFERENCES "showtimes"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "showtime_seats" ADD CONSTRAINT "showtime_seats_held_by_user_id_fkey" FOREIGN KEY ("held_by_user_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "orders" ADD CONSTRAINT "orders_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "orders" ADD CONSTRAINT "orders_showtime_id_fkey" FOREIGN KEY ("showtime_id") REFERENCES "showtimes"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "orders" ADD CONSTRAINT "orders_combo_id_fkey" FOREIGN KEY ("combo_id") REFERENCES "combos"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "order_seats" ADD CONSTRAINT "order_seats_order_id_fkey" FOREIGN KEY ("order_id") REFERENCES "orders"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "order_seats" ADD CONSTRAINT "order_seats_showtime_seat_id_fkey" FOREIGN KEY ("showtime_seat_id") REFERENCES "showtime_seats"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "payments" ADD CONSTRAINT "payments_order_id_fkey" FOREIGN KEY ("order_id") REFERENCES "orders"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "tickets" ADD CONSTRAINT "tickets_order_id_fkey" FOREIGN KEY ("order_id") REFERENCES "orders"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

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

COMMIT;
