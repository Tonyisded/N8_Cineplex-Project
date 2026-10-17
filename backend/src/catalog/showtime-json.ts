import { PrismaService } from "../prisma.service.js";
import type { Prisma } from "../generated/prisma/client.js";

export const slotSelect = {
  id: true,
  movieId: true,
  sourceKey: true,
  startAt: true,
  endAt: true,
  format: true,
  presentationLabel: true,
  basePrice: true,
  status: true,
  auditorium: {
    select: { id: true, name: true, type: true, capacity: true, isExtra: true },
  },
} as const;
type CatalogSlot = Prisma.ShowtimeGetPayload<{ select: typeof slotSelect }>;
export async function slotJson(prisma: PrismaService, items: CatalogSlot[]) {
  if (!items.length) return [];
  const grouped = await prisma.showtimeSeat.groupBy({
    by: ["showtimeId", "status"],
    where: { showtimeId: { in: items.map((s) => s.id) } },
    _count: { _all: true },
  });
  const counts = new Map<
    string,
    { availableSeats: number; totalSeats: number }
  >();
  for (const row of grouped) {
    const count = counts.get(row.showtimeId) ?? {
      availableSeats: 0,
      totalSeats: 0,
    };
    count.totalSeats += row._count._all;
    if (row.status === "AVAILABLE") count.availableSeats += row._count._all;
    counts.set(row.showtimeId, count);
  }
  return items.map((s) => ({
    ...s,
    basePrice: s.basePrice.toString(),
    ...(counts.get(s.id) ?? { availableSeats: 0, totalSeats: 0 }),
  }));
}
