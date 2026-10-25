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
import { ShowtimeSubscriptions } from "./showtime-subscriptions.js";

export type BookingEvent = {
  room: string;
  name: string;
  data: Record<string, unknown>;
};
@Injectable()
@WebSocketGateway({ path: "/socket.io/", maxHttpBufferSize: 4096 })
export class BookingGateway implements OnGatewayInit {
  private readonly subscriptions = new ShowtimeSubscriptions();
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
  join(@ConnectedSocket() socket: Socket, @MessageBody() id: unknown) {
    return this.subscriptions.join(socket, id, async (canonicalId) =>
      Boolean(
        await this.prisma.showtime.findUnique({
          where: { id: canonicalId },
          select: { id: true },
        }),
      ),
    );
  }
  publish(events: BookingEvent[]) {
    for (const event of events)
      this.server?.to(event.room).emit(event.name, event.data);
  }
}
