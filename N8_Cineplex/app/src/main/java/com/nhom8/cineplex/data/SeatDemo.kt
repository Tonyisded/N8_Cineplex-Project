package com.nhom8.cineplex.data

/** Local seat demonstration. It never writes to the booking database. */
enum class SeatKind(val label: String) { STANDARD("Thường"), VIP("VIP"), COUPLE("Couple") }
enum class SeatAvailability { AVAILABLE, HELD, SOLD }

data class DemoSeat(
    val id: String,
    val codes: List<String>,
    val row: Char,
    val kind: SeatKind,
    val price: Long,
    val availability: SeatAvailability = SeatAvailability.AVAILABLE,
) {
    val places get() = codes.size
    val label get() = codes.joinToString("–")
}

object SeatDemo {
    fun initial(): List<DemoSeat> = buildList {
        for (row in 'A'..'J') {
            val kind = when (row) { in 'A'..'D' -> SeatKind.STANDARD; in 'E'..'H' -> SeatKind.VIP; else -> SeatKind.COUPLE }
            val price = when (kind) { SeatKind.STANDARD -> 90_000L; SeatKind.VIP -> 120_000L; SeatKind.COUPLE -> 240_000L }
            for (number in 1..6 step if (kind == SeatKind.COUPLE) 2 else 1) {
                val codes = if (kind == SeatKind.COUPLE) listOf("$row$number", "$row${number + 1}") else listOf("$row$number")
                val id = codes.joinToString("-")
                add(DemoSeat(id, codes, row, kind, price))
            }
        }
    }
}
