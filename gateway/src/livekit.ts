export type LiveKitSelections = {
  engine: "gemini" | "openai";
  actionBackend: "cloud" | "openclaw";
};

/** Allow-list untrusted ticket request values before they become signed room
 * metadata. Unknown values safely retain the existing cloud defaults. */
export function liveKitSelections(body: unknown): LiveKitSelections {
  const value = body && typeof body === "object" ? body as Record<string, unknown> : {};
  return {
    engine: value.engine === "openai" ? "openai" : "gemini",
    actionBackend: value.actionBackend === "openclaw" ? "openclaw" : "cloud",
  };
}
