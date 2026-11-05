import { createHash } from "node:crypto";
import { safeProviderCode, safeTransportCode } from "./refund-retry.js";
import { ProviderGovernor } from "./provider-governor.js";
import { ProxyAgent, fetch as proxyFetch } from "undici";
import { Injectable } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import {
  PayOS,
  APIError,
  ConnectionError,
  ConnectionTimeoutError,
  UserAbortError,
  InvalidSignatureError,
  type CreatePaymentLinkRequest,
  type PayoutRequest,
  type Webhook,
} from "@payos/node";
type PayoutDiagnostics = {
  status?: number;
  signaturePresent?: boolean;
  failure?: Error;
};
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
  #client(kind: "COLLECT" | "PAYOUT", diagnostic?: PayoutDiagnostics) {
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
        try {
          const response = await (transport.fetch ?? globalThis.fetch)(url, {
            ...init,
            signal: AbortSignal.any([
              ...(init?.signal ? [init.signal] : []),
              AbortSignal.timeout(15000),
            ]),
          });
          if (diagnostic) {
            diagnostic.status = response.status;
            diagnostic.signaturePresent = !!response.headers.get("x-signature");
          }
          return response;
        } catch (error) {
          if (!diagnostic) throw error;
          const e = error as { name?: string };
          const transportCode = safeTransportCode(error);
          const timeout =
            e?.name === "TimeoutError" ||
            init?.signal?.reason?.name === "TimeoutError" ||
            [
              "ETIMEDOUT",
              "UND_ERR_CONNECT_TIMEOUT",
              "UND_ERR_HEADERS_TIMEOUT",
              "UND_ERR_BODY_TIMEOUT",
            ].includes(transportCode ?? "");
          diagnostic.failure = Object.assign(
            timeout
              ? new ConnectionTimeoutError("Payout transport timed out")
              : init?.signal?.aborted
                ? new UserAbortError("Payout request aborted")
                : new ConnectionError("Payout transport failed"),
            { transportCode },
          );
          throw diagnostic.failure;
        }
      },
    });
  }
  private async payout<T>(call: (client: PayOS) => Promise<T>): Promise<T> {
    const diagnostic: PayoutDiagnostics = {};
    let result: T;
    try {
      result = await call(this.#client("PAYOUT", diagnostic));
    } catch (error) {
      // SDK replaces fetch exceptions; keep request-scoped safe evidence outside it.
      if (diagnostic.failure) throw diagnostic.failure;
      const e = error as { status?: number; code?: unknown; headers?: Headers };
      const retry = e?.headers?.get?.("retry-after");
      const headers = new Headers();
      if (retry && /^\d{1,10}$/.test(retry)) headers.set("retry-after", retry);
      else if (retry && Number.isFinite(Date.parse(retry)))
        headers.set("retry-after", new Date(retry).toUTCString());
      const status = diagnostic.status ?? e?.status;
      const payload = {
        code: safeProviderCode(e?.code),
        desc: "Payout API request failed",
      };
      const safe =
        error instanceof InvalidSignatureError
          ? new InvalidSignatureError("Payout signature validation failed")
          : status === undefined
            ? new APIError(
                undefined,
                payload,
                "Payout request setup failed",
                headers,
              )
            : APIError.generateError(
                status,
                payload,
                "Payout API request failed",
                headers,
              );
      throw Object.assign(safe, {
        status,
        signaturePresent: diagnostic.signaturePresent,
      });
    }
    if (!diagnostic.signaturePresent)
      throw Object.assign(
        new InvalidSignatureError("Missing payout signature"),
        { status: diagnostic.status, signaturePresent: false },
      );
    return result;
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
      return this.payout((client) => client.payouts.create(data, key));
    });
  }
  getPayout(id: string) {
    return this.run("PAYOUT", "PAYOUT_GET", () =>
      this.payout((client) => client.payouts.get(id)),
    );
  }
  payoutBalance() {
    return this.run("PAYOUT", "PAYOUT_BALANCE", () =>
      this.payout((client) => client.payoutsAccount.balance()),
    );
  }
  async findPayout(referenceId: string) {
    const page = await this.run("PAYOUT", "PAYOUT_LIST", () =>
      this.payout((client) => client.payouts.list({ referenceId, limit: 10 })),
    );
    return page.data.filter((p) => p.referenceId === referenceId);
  }
}
