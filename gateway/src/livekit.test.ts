import assert from "node:assert/strict";
import test from "node:test";
import { liveKitSelections } from "./livekit.js";

test("accepts the OpenClaw action backend", () => {
  assert.deepEqual(liveKitSelections({ engine: "openai", actionBackend: "openclaw" }), {
    engine: "openai",
    actionBackend: "openclaw",
  });
});

test("unknown values fail closed to Sean's current defaults", () => {
  assert.deepEqual(liveKitSelections({ engine: "other", actionBackend: "http://private" }), {
    engine: "gemini",
    actionBackend: "cloud",
  });
});
