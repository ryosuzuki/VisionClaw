import asyncio
import json
import unittest
from types import SimpleNamespace

from openclaw_relay import phone_openclaw_execute, resolve_openclaw_response


class FakeParticipant:
    def __init__(self):
        self.messages = []

    async def send_text(self, text, **kwargs):
        self.messages.append((json.loads(text), kwargs))


class FakeRoom:
    def __init__(self):
        self.local_participant = FakeParticipant()


class OpenClawRelayTest(unittest.IsolatedAsyncioTestCase):
    async def test_correlates_success_without_credentials_on_wire(self):
        room = FakeRoom()
        userdata = SimpleNamespace(user_id="phone", room=room, openclaw_pending={})
        task = asyncio.create_task(phone_openclaw_execute(userdata, "add a reminder", None))
        await asyncio.sleep(0)
        payload, options = room.local_participant.messages[0]
        self.assertEqual(payload["task"], "add a reminder")
        self.assertNotIn("token", payload)
        self.assertNotIn("url", payload)
        self.assertEqual(options["destination_identities"], ["phone"])
        resolve_openclaw_response(
            userdata,
            json.dumps({"id": payload["id"], "ok": True, "result": "done"}),
        )
        self.assertEqual(await task, "done")
        self.assertEqual(userdata.openclaw_pending, {})

    async def test_propagates_phone_error(self):
        room = FakeRoom()
        userdata = SimpleNamespace(user_id="phone", room=room, openclaw_pending={})
        task = asyncio.create_task(phone_openclaw_execute(userdata, "task", None))
        await asyncio.sleep(0)
        request_id = room.local_participant.messages[0][0]["id"]
        resolve_openclaw_response(
            userdata,
            json.dumps({"id": request_id, "ok": False, "error": "offline"}),
        )
        with self.assertRaisesRegex(RuntimeError, "offline"):
            await task


if __name__ == "__main__":
    unittest.main()
