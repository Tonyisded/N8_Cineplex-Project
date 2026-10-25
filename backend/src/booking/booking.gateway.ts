import { Injectable } from "@nestjs/common";
import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  ConnectedSocket,
  MessageBody,
  type OnGatewayInit,
} from "@nestjs/websockets";
import { Server, Socket } from "socket.io";
import { AuthService } from "../auth/auth.service.js";
import { PrismaService } from "../prisma.service.js";

export type BookingEvent = {
  room: string;
  name: string;
  data: Record<string, unknown>;
};
@Injectable()
@WebSocketGateway({ path: "/socket.io/", maxHttpBufferSize: 4096 })
export class BookingGateway implements OnGatewayInit {
  @WebSocketServer() server!: Server;
  constructor(
    private readonly auth: AuthService,
    private readonly prisma: PrismaService,
  ) {}
  afterInit(server: Server) {
    server.use(async (socket, next) => {
      try {
        const token = socket.handshake.auth?.token;
        if (typeof token !== "string") throw new Error();
        const user = await this.auth.authenticate(token);
        if (user.role !== "CUSTOMER") throw new Error();
        socket.data.userId = user.id;
        const payload = JSON.parse(
          Buffer.from(token.split(".")[1], "base64url").toString(),
        );
        const timer = setTimeout(
          () => socket.disconnect(true),
          Math.max(0, payload.exp * 1000 - Date.now()),
        );
        timer.unref();
        socket.once("disconnect", () => clearTimeout(timer));
        await socket.join(`user:${user.id}`);
        next();
      } catch {
        next(new Error("AUTH_REQUIRED"));
      }
    });
  }
  @SubscribeMessage("JOIN_SHOWTIME")
  async join(@ConnectedSocket() socket: Socket, @MessageBody() id: unknown) {
    if (
      typeof id !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
        id,
      )
    )
      return { ok: false };
    if (
      !(await this.prisma.showtime.findUnique({
        where: { id },
        select: { id: true },
      }))
    )
      return { ok: false };
    for (const room of socket.rooms)
      if (room.startsWith("showtime:")) await socket.leave(room);
    await socket.join(`showtime:${id}`);
    return { ok: true };
  }
  publish(events: BookingEvent[]) {
    for (const event of events)
      this.server?.to(event.room).emit(event.name, event.data);
  }
}
