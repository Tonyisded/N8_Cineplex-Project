import { createHash } from "node:crypto";
import { ProviderGovernor } from "./provider-governor.js";
import { ProxyAgent, fetch as proxyFetch } from "undici";
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
  constructor(
    private readonly config: ConfigService,
    private readonly governor: ProviderGovernor,
  ) {}
  private run<T>(
    kind: "COLLECT" | "PAYOUT",
    operation: string,
    call: () => Promise<T>,
  ) {
    if (!this.governor) throw new Error("Provider governor is required");
    const key =
      kind +
      ":" +
      createHash("sha256")
        .update(this.config.getOrThrow<string>(`PAYOS_${kind}_CLIENT_ID`))
        .digest("hex");
    return this.governor.execute(
      key,
      operation,
      kind === "PAYOUT" ? 5000 : 1000,
      call,
    );
  }
  private payoutAgent?: ProxyAgent;
  async onModuleDestroy() {
    await this.payoutAgent?.close();
  }
  private payoutTransport() {
    const enabled = this.config.get("PAYOS_PAYOUT_PROXY_ENABLED");
    if (enabled !== true && enabled !== "true") return {};
    if (!this.payoutAgent) {
      const username =
        this.config.get<string>("PAYOS_PAYOUT_PROXY_USERNAME") ?? "";
      const password =
        this.config.get<string>("PAYOS_PAYOUT_PROXY_PASSWORD") ?? "";
      this.payoutAgent = new ProxyAgent({
        uri: this.config.getOrThrow<string>("PAYOS_PAYOUT_PROXY_SERVER"),
        ...(username
          ? {
              token:
                "Basic " +
                Buffer.from(username + ":" + password).toString("base64"),
            }
          : {}),
      });
    }
    return {
      fetch: proxyFetch as unknown as typeof globalThis.fetch,
      fetchOptions: {
        dispatcher: this.payoutAgent,
        redirect: "error",
      } as RequestInit,
    };
  }
  #client(kind: "COLLECT" | "PAYOUT") {
    const prefix = `PAYOS_${kind}_`;
    return new PayOS({
      clientId: this.config.getOrThrow<string>(prefix + "CLIENT_ID"),
      apiKey: this.config.getOrThrow<string>(prefix + "API_KEY"),
      checksumKey: this.config.getOrThrow<string>(prefix + "CHECKSUM_KEY"),
      maxRetries: 0,
      timeout: 15000,
      logLevel: "off",
      ...(kind === "PAYOUT" ? this.payoutTransport() : {}),
      fetch: async (url, init) => {
        const transport = kind === "PAYOUT" ? this.payoutTransport() : {};
        const response = await (transport.fetch ?? globalThis.fetch)(url, {
          ...init,
          signal: AbortSignal.any([
            ...(init?.signal ? [init.signal] : []),
            AbortSignal.timeout(15000),
          ]),
        });
        if (
          kind === "PAYOUT" &&
          response.ok &&
          !response.headers.get("x-signature")
        )
          throw new Error("Missing payout signature");
        return response;
      },
    });
  }
  createLink(data: CreatePaymentLinkRequest) {
    return this.run("COLLECT", "COLLECT_CREATE", () =>
      this.#client("COLLECT").paymentRequests.create(data),
    );
  }
  getLink(code: number) {
    return this.run("COLLECT", "COLLECT_GET", () =>
      this.#client("COLLECT").paymentRequests.get(code),
    );
  }
  cancelLink(code: number) {
    return this.run("COLLECT", "COLLECT_CANCEL", () =>
      this.#client("COLLECT").paymentRequests.cancel(code, "N8 hold closed"),
    );
  }
  verify(body: unknown) {
    return this.#client("COLLECT").webhooks.verify(body as Webhook);
  }
  createPayout(
    data: PayoutRequest,
    key: string,
    beforeSend?: () => Promise<void>,
  ) {
    return this.run("PAYOUT", "PAYOUT_CREATE", async () => {
      await beforeSend?.();
      return this.#client("PAYOUT").payouts.create(data, key);
    });
  }
  getPayout(id: string) {
    return this.run("PAYOUT", "PAYOUT_GET", () =>
      this.#client("PAYOUT").payouts.get(id),
    );
  }
  payoutBalance() {
    return this.run("PAYOUT", "PAYOUT_BALANCE", () =>
      this.#client("PAYOUT").payoutsAccount.balance(),
    );
  }
  async findPayout(referenceId: string) {
    const page = await this.run("PAYOUT", "PAYOUT_LIST", () =>
      this.#client("PAYOUT").payouts.list({ referenceId, limit: 10 }),
    );
    return page.data.filter((p) => p.referenceId === referenceId);
  }
}
