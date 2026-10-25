export interface SubscriptionSocket {
  connected: boolean;
  rooms: Set<string>;
  join(room: string): void | Promise<void>;
  leave(room: string): void | Promise<void>;
}

type Reply = { ok: true } | { ok: false; code?: string };
type SubscriptionState = {
  queue: Promise<unknown>;
  windowStart: number;
  requests: number;
};

const uuid =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

// Only authenticated Socket.IO connections reach this policy through the
// gateway. Each connection may follow one showtime while retaining its user room.
export class ShowtimeSubscriptions {
  private readonly states = new WeakMap<SubscriptionSocket, SubscriptionState>();

  join(
    socket: SubscriptionSocket,
    value: unknown,
    exists: (id: string) => Promise<boolean>,
  ): Promise<Reply> {
    if (!socket.connected)
      return Promise.resolve({ ok: false, code: "AUTH_REQUIRED" });
    let state = this.states.get(socket);
    if (!state) {
      state = { queue: Promise.resolve(), windowStart: Date.now(), requests: 0 };
      this.states.set(socket, state);
    }
    if (Date.now() - state.windowStart >= 60_000) {
      state.windowStart = Date.now();
      state.requests = 0;
    }
    // Bound the queue and database work even when packets arrive concurrently.
    if (++state.requests > 30)
      return Promise.resolve({ ok: false, code: "RATE_LIMITED" });
    if (typeof value !== "string" || !uuid.test(value))
      return Promise.resolve({ ok: false });
    const id = value.toLowerCase();
    const next = state.queue
      .then(() => this.switchRoom(socket, id, exists))
      .catch((): Reply => ({ ok: false, code: "REALTIME_UNAVAILABLE" }));
    state.queue = next;
    return next;
  }

  private async switchRoom(
    socket: SubscriptionSocket,
    id: string,
    exists: (id: string) => Promise<boolean>,
  ): Promise<Reply> {
    if (!socket.connected) return { ok: false, code: "AUTH_REQUIRED" };
    if (!(await exists(id))) return { ok: false };
    if (!socket.connected) return { ok: false, code: "AUTH_REQUIRED" };
    for (const room of [...socket.rooms]) {
      if (room.startsWith("showtime:") && room !== `showtime:${id}`) {
        await socket.leave(room);
        if (!socket.connected) return { ok: false, code: "AUTH_REQUIRED" };
      }
    }
    await socket.join(`showtime:${id}`);
    if (!socket.connected) {
      // An asynchronous adapter may finish joining after the transport closed.
      await socket.leave(`showtime:${id}`);
      return { ok: false, code: "AUTH_REQUIRED" };
    }
    return { ok: true };
  }
}
