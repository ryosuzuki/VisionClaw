export type LiveKitSelections = {
  engine: "gemini" | "openai";
  actionBackend: "cloud" | "openclaw";
};

/** Allow-list untrusted ticket request values before signing room metadata. */
export function liveKitSelections(body: unknown): LiveKitSelections {
  const value = body && typeof body === "object" ? body as Record<string, unknown> : {};
  return {
    engine: value.engine === "openai" ? "openai" : "gemini",
    actionBackend: value.actionBackend === "openclaw" ? "openclaw" : "cloud",
  };
}
