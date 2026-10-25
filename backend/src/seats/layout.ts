import type { AuditoriumType, SeatType } from "../generated/prisma/enums.js";

const rows = "ABCDEFGHJKLMNOPQRSTUVWXYZ";
const standard: Record<number, number[]> = {
  160: [12, 14, 14, 14, 15, 15, 15, 15, 15, 15, 16],
  145: [13, 14, 14, 14, 14, 14, 14, 16, 16, 16],
  130: [12, 14, 14, 14, 14, 14, 16, 16, 16],
  112: [12, 12, 14, 14, 14, 14, 16, 16],
};
function counts(type: AuditoriumType, capacity: number): number[] {
  if (type === "STANDARD" && standard[capacity]) return standard[capacity];
  if (type === "IMAX" && capacity === 496)
    return [18, 22, 26, 30, 32, 34, 36, 36, 38, 38, 38, 38, 38, 36, 36];
  if (type === "SCREENX" && capacity === 180) return Array(10).fill(18);
  if (type === "GOLD" && capacity === 32) return [8, 8, 8, 8];
  // shortcut: custom capacities retain the legacy grid until a room template is approved.
  const width = { STANDARD: 16, IMAX: 31, SCREENX: 12, GOLD: 8 }[type];
  if (
    !Number.isInteger(capacity) ||
    capacity < 1 ||
    capacity > width * rows.length
  )
    throw new Error("Invalid auditorium capacity");
  return Array.from({ length: Math.ceil(capacity / width) }, (_, i) =>
    Math.min(width, capacity - i * width),
  );
}

export function seatTemplate(type: AuditoriumType, capacity: number) {
  const sizes = counts(type, capacity);
  const zoned =
    type === "STANDARD"
      ? !!standard[capacity]
      : type === "SCREENX" && capacity === 180;
  return sizes.flatMap((count, row) => {
    const seatType: SeatType =
      type === "GOLD"
        ? "GOLD"
        : type === "IMAX"
          ? "VIP"
          : zoned
            ? row === sizes.length - 1 && count % 2 === 0
              ? "COUPLE"
              : row < 3
                ? "NORMAL"
                : "VIP"
            : "NORMAL";
    return Array.from({ length: count }, (_, i) => ({
      seatCode: `${rows[row]}${i + 1}`,
      rowName: rows[row],
      seatNumber: i + 1,
      seatType,
    }));
  });
}

type Seat = {
  id: string;
  seatCode: string;
  rowName: string;
  seatNumber: number;
  seatType: string;
};
export function seatLayout(
  type: AuditoriumType,
  capacity: number,
  inventory: Seat[],
) {
  const expected = seatTemplate(type, capacity);
  const byCode = new Map(inventory.map((s) => [s.seatCode, s]));
  const canonical =
    expected.length === inventory.length &&
    expected.every((s) => byCode.get(s.seatCode)?.seatType === s.seatType);
  const groups = [...new Set(inventory.map((s) => s.rowName))]
    .sort()
    .map((row) =>
      inventory
        .filter((s) => s.rowName === row)
        .sort((a, b) => b.seatNumber - a.seatNumber),
    );
  const extra = (n: number) =>
    type === "GOLD"
      ? Math.floor((n - 1) / 2) * 0.9
      : canonical && (type === "IMAX" || type === "SCREENX")
        ? 2
        : 0;
  const width =
    Math.max(8, ...groups.map((g) => g.length + extra(g.length))) + 3;
  const height = groups.length * 1.15 + 4;
  return {
    key: `${type}-${capacity}-${canonical ? "v1" : "legacy"}`,
    width,
    height,
    screens: type === "SCREENX" ? ["FRONT", "LEFT", "RIGHT"] : ["FRONT"],
    seats: groups.flatMap((group, ri) => {
      const left = (width - group.length - extra(group.length)) / 2;
      const side = type === "IMAX" ? (group.length - 14) / 2 : 6;
      return group.map((s, i) => ({
        seatId: s.id,
        x:
          left +
          i +
          (type === "GOLD"
            ? Math.floor(i / 2) * 0.9
            : canonical && (type === "IMAX" || type === "SCREENX")
              ? (i >= side ? 1 : 0) +
                (i >= side + (type === "IMAX" ? 14 : 6) ? 1 : 0)
              : 0),
        y: 2.4 + ri * 1.15,
        pairKey:
          s.seatType === "COUPLE"
            ? `${s.rowName}${Math.floor((s.seatNumber - 1) / 2) * 2 + 1}`
            : null,
      }));
    }),
  };
}
