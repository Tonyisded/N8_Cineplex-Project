import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { GoogleAuth } from "google-auth-library";
import { PrismaService } from "../prisma.service.js";

@Injectable()
export class FcmService {
  constructor(
    private readonly config: ConfigService,
    private readonly prisma: PrismaService,
  ) {}
  async send(
    userId: string,
    token: string,
    title: string,
    body: string,
    ticketId: string,
  ): Promise<boolean> {
    if (!this.config.get<boolean>("FCM_ENABLED")) return false;
    try {
      const auth = new GoogleAuth({
        credentials: {
          client_email: this.config.getOrThrow<string>("FIREBASE_CLIENT_EMAIL"),
          private_key: this.config
            .getOrThrow<string>("FIREBASE_PRIVATE_KEY")
            .replace(/\\n/g, "\n"),
        },
        scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
      });
      const client = await auth.getClient();
      const access = await client.getAccessToken();
      if (!access.token) return false;
      const project = this.config.getOrThrow<string>("FIREBASE_PROJECT_ID");
      const response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(project)}/messages:send`,
        {
          method: "POST",
          headers: {
            authorization: `Bearer ${access.token}`,
            "content-type": "application/json",
          },
          body: JSON.stringify({
            message: {
              token,
              notification: { title, body },
              data: { ticketId, screen: "TICKET" },
              android: {
                notification: {
                  channel_id: "cineplex_transactions",
                  tag: `ticket-${ticketId}`,
                },
              },
            },
          }),
          signal: AbortSignal.timeout(10000),
        },
      );
      if (response.ok) return true;
      const error = (await response.json().catch(() => ({}))) as {
        error?: { details?: { errorCode?: string }[] };
      };
      if (error.error?.details?.some((d) => d.errorCode === "UNREGISTERED")) {
        await this.prisma.user.updateMany({
          where: { id: userId, fcmToken: token },
          data: { fcmToken: null },
        });
      }
      return false;
    } catch {
      return false;
    }
  }
}
