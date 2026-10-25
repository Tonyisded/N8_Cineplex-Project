import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { setImmediate as turn } from "node:timers/promises";
import { ShowtimeSubscriptions } from "../src/booking/showtime-subscriptions.ts";

function fixture() {
  const rooms = new Set(["socket-id", "user:owner"]);
  const socket = {
    connected: true,
    rooms,
    async join(room) { rooms.add(room); },
    async leave(room) { rooms.delete(room); },
  };
  const policy = new ShowtimeSubscriptions();
  return { socket, rooms, policy };
}

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}

test("canonical UUID rooms preserve the socket and authenticated owner room", async () => {
  const f = fixture();
  const id = "abcdefff-1234-4000-8000-123456abcdef";
  const result = await f.policy.join(f.socket, id.toUpperCase(), async (value) => value === id);
  assert.deepEqual(result, { ok: true });
  assert.deepEqual([...f.rooms], ["socket-id", "user:owner", `showtime:${id}`]);
});

test("invalid room names, payload objects and missing showtimes cannot add rooms", async () => {
  const f = fixture();
  let reads = 0;
  const exists = async () => { reads++; return false; };
  for (const value of [null, [], {}, { userId: "victim" }, "user:victim", "invalid"]) {
    assert.equal((await f.policy.join(f.socket, value, exists)).ok, false);
  }
  assert.equal(reads, 0);
  assert.equal((await f.policy.join(f.socket, randomUUID(), exists)).ok, false);
  assert.deepEqual([...f.rooms], ["socket-id", "user:owner"]);
});

test("concurrent asynchronous joins leave exactly the last selected showtime", async () => {
  const f = fixture();
  const a = randomUUID(), b = randomUUID();
  const gate = deferred(), entered = deferred();
  const join = f.socket.join.bind(f.socket);
  f.socket.join = async (room) => {
    if (room === `showtime:${a}`) { entered.resolve(); await gate.promise; }
    await join(room);
  };
  const first = f.policy.join(f.socket, a, async () => true);
  await entered.promise;
  const second = f.policy.join(f.socket, b, async () => true);
  await turn();
  gate.resolve();
  assert.ok((await Promise.all([first, second])).every((result) => result.ok));
  assert.deepEqual([...f.rooms], ["socket-id", "user:owner", `showtime:${b}`]);
});

test("disconnect while validating a showtime prevents any room join", async () => {
  const f = fixture();
  const gate = deferred(), entered = deferred();
  const pending = f.policy.join(f.socket, randomUUID(), async () => {
    entered.resolve(); await gate.promise; return true;
  });
  await entered.promise;
  f.socket.connected = false;
  f.rooms.clear();
  gate.resolve();
  assert.equal((await pending).ok, false);
  assert.equal(f.rooms.size, 0);
});

test("disconnect during an adapter join removes late membership", async () => {
  const f = fixture();
  const gate = deferred(), entered = deferred();
  const id = randomUUID();
  f.socket.join = async (room) => {
    entered.resolve(); await gate.promise; f.rooms.add(room);
  };
  const pending = f.policy.join(f.socket, id, async () => true);
  await entered.promise;
  f.socket.connected = false;
  f.rooms.clear();
  gate.resolve();
  assert.equal((await pending).ok, false);
  assert.equal(f.rooms.size, 0);
});

test("a failed lookup does not poison later subscriptions", async () => {
  const f = fixture();
  assert.equal((await f.policy.join(f.socket, randomUUID(), async () => { throw Error("fixture unavailable"); })).ok, false);
  const id = randomUUID();
  assert.equal((await f.policy.join(f.socket, id, async () => true)).ok, true);
  assert.ok(f.rooms.has(`showtime:${id}`));
});

test("rate limit bounds concurrent work per socket and recovers after a minute", async (t) => {
  t.mock.timers.enable({ apis: ["Date"], now: 100000 });
  const f = fixture();
  let reads = 0;
  const id = randomUUID();
  const exists = async () => { reads++; return true; };
  const results = await Promise.all(Array.from({ length: 50 }, () => f.policy.join(f.socket, id, exists)));
  assert.equal(results.filter((result) => result.ok).length, 30);
  assert.equal(results.filter((result) => result.code === "RATE_LIMITED").length, 20);
  assert.equal(reads, 30);
  t.mock.timers.tick(60000);
  assert.equal((await f.policy.join(f.socket, id, exists)).ok, true);
});

test("a blocked socket does not block another authenticated connection", async () => {
  const f = fixture();
  const other = fixture();
  const gate = deferred();
  const entered = deferred();
  const first = f.policy.join(f.socket, randomUUID(), async () => {
    entered.resolve();
    await gate.promise;
    return true;
  });
  await entered.promise;
  try {
    const id = randomUUID();
    assert.deepEqual(
      await f.policy.join(other.socket, id, async () => true),
      { ok: true },
    );
    assert.ok(other.rooms.has(`showtime:${id}`));
    assert.deepEqual([...f.rooms], ["socket-id", "user:owner"]);
  } finally {
    gate.resolve();
    await first;
  }
});

test("an unavailable showtime preserves the current subscription and private room", async () => {
  const f = fixture();
  const current = randomUUID();
  await f.policy.join(f.socket, current, async () => true);
  const before = [...f.rooms];
  assert.equal(
    (await f.policy.join(f.socket, randomUUID(), async () => false)).ok,
    false,
  );
  assert.deepEqual([...f.rooms], before);
});
