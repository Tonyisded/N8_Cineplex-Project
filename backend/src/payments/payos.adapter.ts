import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import {
  PayOS,
  type CreatePaymentLinkRequest,
  type PayoutRequest,
  type Webhook,
} from "@payos/node";
@Injectable()
export class PayosAdapter {
  constructor(private readonly config: ConfigService) {}
  private client(kind: "COLLECT" | "PAYOUT") {
    const prefix = `PAYOS_${kind}_`;
    return new PayOS({
      clientId: this.config.getOrThrow<string>(prefix + "CLIENT_ID"),
      apiKey: this.config.getOrThrow<string>(prefix + "API_KEY"),
      checksumKey: this.config.getOrThrow<string>(prefix + "CHECKSUM_KEY"),
      maxRetries: 0,
      timeout: 15000,
      logLevel: "off",
    });
  }
  createLink(data: CreatePaymentLinkRequest) {
    return this.client("COLLECT").paymentRequests.create(data);
  }
  getLink(code: number) {
    return this.client("COLLECT").paymentRequests.get(code);
  }
  cancelLink(code: number) {
    return this.client("COLLECT").paymentRequests.cancel(
      code,
      "N8 hold closed",
    );
  }
  verify(body: unknown) {
    return this.client("COLLECT").webhooks.verify(body as Webhook);
  }
  createPayout(data: PayoutRequest, key: string) {
    return this.client("PAYOUT").payouts.create(data, key);
  }
  getPayout(id: string) {
    return this.client("PAYOUT").payouts.get(id);
  }
  async findPayout(referenceId: string) {
    const page = await this.client("PAYOUT").payouts.list({
      referenceId,
      limit: 10,
    });
    return page.data.filter((p) => p.referenceId === referenceId);
  }
}
