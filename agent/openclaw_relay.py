"""Credential-free LiveKit request/response relay for phone-owned OpenClaw."""

from __future__ import annotations

import asyncio
import json
import uuid
from typing import Any

OPENCLAW_REQUEST_TOPIC = "vc.openclaw.request"
OPENCLAW_RESPONSE_TOPIC = "vc.openclaw.response"
OPENCLAW_RELAY_TIMEOUT_S = 330


async def phone_openclaw_execute(userdata: Any, task: str, image_b64: str | None) -> str:
    if userdata.room is None:
        raise RuntimeError("OpenClaw relay room is unavailable")
    request_id = uuid.uuid4().hex
    future: asyncio.Future[str] = asyncio.get_running_loop().create_future()
    userdata.openclaw_pending[request_id] = future
    payload: dict[str, str] = {"id": request_id, "task": task}
    if image_b64:
        payload["image"] = image_b64
    try:
        await userdata.room.local_participant.send_text(
            json.dumps(payload),
            topic=OPENCLAW_REQUEST_TOPIC,
            destination_identities=[userdata.user_id],
        )
        return await asyncio.wait_for(future, timeout=OPENCLAW_RELAY_TIMEOUT_S)
    finally:
        userdata.openclaw_pending.pop(request_id, None)


def resolve_openclaw_response(userdata: Any, raw: str) -> None:
    body = json.loads(raw)
    request_id = str(body.get("id", ""))
    future = userdata.openclaw_pending.get(request_id)
    if future is None or future.done():
        return
    if body.get("ok") is True:
        future.set_result(str(body.get("result", "")))
    else:
        future.set_exception(RuntimeError(str(body.get("error", "OpenClaw request failed"))))
