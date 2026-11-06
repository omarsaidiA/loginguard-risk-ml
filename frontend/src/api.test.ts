import { afterEach, describe, expect, it, vi } from "vitest";

import { api, ApiError, readCsrfToken } from "./api";

function mockFetch(status: number, body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(status === 204 ? null : JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
});

describe("readCsrfToken", () => {
  it("reads the token from the cookie the server sets", () => {
    expect(readCsrfToken("a=1; XSRF-TOKEN=abc%3D123; b=2")).toBe("abc=123");
    expect(readCsrfToken("a=1")).toBeUndefined();
  });
});

describe("api client", () => {
  it("sends the CSRF token on state-changing requests only", async () => {
    document.cookie = "XSRF-TOKEN=token-1";
    const fetchMock = mockFetch(200, { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });

    await api.events({});
    expect(fetchMock.mock.calls[0]?.[1].headers["X-XSRF-TOKEN"]).toBeUndefined();

    mockFetch(200, {}).mockClear();
    const postMock = mockFetch(200, {});
    await api.reviewEvent(1, "LEGITIMATE");
    expect(postMock.mock.calls[0]?.[1].headers["X-XSRF-TOKEN"]).toBe("token-1");
    expect(postMock.mock.calls[0]?.[1].credentials).toBe("same-origin");
  });

  it("turns problem details into an ApiError with the invalid fields", async () => {
    mockFetch(400, { detail: "Invalid request", errors: [{ field: "ipAddress", message: "must be valid" }] });
    const error = await api.recordEvent({
      userId: 1,
      occurredAt: "2026-01-01T00:00:00Z",
      ipAddress: "nope",
      countryCode: "FR",
      deviceId: "d",
      successful: true,
    }).catch((caught: unknown) => caught);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).fieldErrors[0]?.field).toBe("ipAddress");
  });

  it("only builds query parameters for the filters that are set", async () => {
    const fetchMock = mockFetch(200, { content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
    await api.events({ level: "HIGH", status: "", page: 2 });
    expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/login-events?level=HIGH&page=2");
  });
});
