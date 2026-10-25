import "reflect-metadata";
import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { setImmediate as turn } from "node:timers/promises";
import { Test } from "@nestjs/testing";
import { io } from "socket.io-client";
import { BookingGateway } from "../dist/booking/booking.gateway.js";
import { AuthService } from "../dist/auth/auth.service.js";
import { PrismaService } from "../dist/prisma.service.js";

function deferred() {
  let resolve;
  const promise = new Promise((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

async function fixture(t, find = async (id) => ({ id })) {
  const userId = randomUUID();
  const token = `fixture.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 60 })).toString("base64url")}.fixture`;
  const module = await Test.createTestingModule({
    providers: [
      BookingGateway,
      {
        provide: AuthService,
        useValue: {
          authenticate: async (value) => {
            if (value !== token) throw Error("Invalid fixture token");
            return { id: userId, role: "CUSTOMER" };
          },
        },
      },
      {
        provide: PrismaService,
        useValue: {
          showtime: {
            findUnique: ({ where }) => find(where.id),
          },
        },
      },
    ],
  }).compile();
  const app = module.createNestApplication({ logger: false });
  await app.listen(0, "127.0.0.1");
  t.after(() => app.close());
  const socket = io(await app.getUrl(), {
    auth: { token },
    forceNew: true,
    reconnection: false,
    timeout: 2000,
    transports: ["websocket"],
  });
  t.after(() => socket.close());
  await new Promise((resolve, reject) => {
    socket.once("connect", resolve);
    socket.once("connect_error", reject);
  });
  const gateway = app.get(BookingGateway);
  const serverSocket = gateway.server.sockets.sockets.get(socket.id);
  assert.ok(serverSocket);
  const ack = (value) =>
    socket.timeout(3000).emitWithAck("JOIN_SHOWTIME", value);
  return { socket, serverSocket, gateway, ack, userId };
}

test(
  "JOIN_SHOWTIME normalizes UUID case and receives the canonical showtime room",
  { timeout: 8000 },
  async (t) => {
    const id = "abcdefff-1234-4000-8000-123456abcdef";
    const f = await fixture(t, async (value) => (value === id ? { id } : null));
    assert.equal((await f.ack(id.toUpperCase())).ok, true);
    assert.ok(f.serverSocket.rooms.has(`showtime:${id}`));
    assert.equal(
      f.serverSocket.rooms.has(`showtime:${id.toUpperCase()}`),
      false,
    );
    const received = new Promise((resolve) =>
      f.socket.once("SEAT_HELD", resolve),
    );
    f.gateway.publish([
      { room: `showtime:${id}`, name: "SEAT_HELD", data: { showtimeId: id } },
    ]);
    assert.deepEqual(await received, { showtimeId: id });
  },
);

test(
  "overlapping room switches leave exactly the last requested showtime",
  { timeout: 8000 },
  async (t) => {
    const a = randomUUID(),
      b = randomUUID();
    const f = await fixture(t);
    const blocked = deferred(),
      entered = deferred(),
      secondReceived = deferred();
    const join = f.serverSocket.join.bind(f.serverSocket);
    f.serverSocket.join = async (room) => {
      if (room === `showtime:${a}`) {
        entered.resolve();
        await blocked.promise;
      }
      return join(room);
    };
    f.serverSocket.onAny((event, value) => {
      if (event === "JOIN_SHOWTIME" && value === b) secondReceived.resolve();
    });
    const firstAck = f.ack(a);
    await entered.promise;
    const secondAck = f.ack(b);
    await secondReceived.promise;
    await turn();
    blocked.resolve();
    const results = await Promise.all([firstAck, secondAck]);
    assert.ok(results.every((result) => result.ok));
    assert.deepEqual(
      [...f.serverSocket.rooms].filter((room) => room.startsWith("showtime:")),
      [`showtime:${b}`],
    );
    assert.ok(f.serverSocket.rooms.has(`user:${f.userId}`));
  },
);

test(
  "disconnect during showtime lookup cannot recreate ghost room membership",
  { timeout: 8000 },
  async (t) => {
    const waiting = deferred(),
      entered = deferred();
    const f = await fixture(t, async (id) => {
      entered.resolve();
      await waiting.promise;
      return { id };
    });
    const id = randomUUID();
    f.socket.emit("JOIN_SHOWTIME", id, () => {});
    await entered.promise;
    const disconnected = new Promise((resolve) =>
      f.serverSocket.once("disconnect", resolve),
    );
    f.socket.close();
    await disconnected;
    waiting.resolve();
    await turn();
    assert.equal(
      f.gateway.server.sockets.adapter.rooms.has(`showtime:${id}`),
      false,
    );
  },
);
