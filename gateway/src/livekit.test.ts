import assert from "node:assert/strict";
import test from "node:test";
import { liveKitSelections } from "./livekit.js";

test("accepts the self-hosted OpenClaw selection", () => {
  assert.deepEqual(liveKitSelections({ engine: "openai", actionBackend: "openclaw" }), {
    engine: "openai",
    actionBackend: "openclaw",
  });
});

test("unknown metadata values fail closed to existing cloud defaults", () => {
  assert.deepEqual(liveKitSelections({ engine: "other", actionBackend: "http://private" }), {
    engine: "gemini",
    actionBackend: "cloud",
  });
});
