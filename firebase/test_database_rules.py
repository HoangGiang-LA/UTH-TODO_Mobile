"""Test profile rules against a disposable local Realtime Database emulator.

Requires Python, Java and the RTDB emulator JAR installed by Firebase CLI.
Never connects to a live Firebase project and does not touch Android emulators.
"""

import argparse
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SERVER_TIME = {".sv": "timestamp"}
ADMIN = object()
BASE_URL = ""


def request(method, path, data=None, identity=ADMIN):
    query = {"ns": "demo-uth-todo-btvn03"}
    if identity is not ADMIN:
        query["auth_variable_override"] = json.dumps(
            None if identity is None else {"uid": identity}, separators=(",", ":")
        )
    url = BASE_URL + "/" + path.strip("/") + ".json?" + urllib.parse.urlencode(query)
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, method=method, headers={
        "Authorization": "Bearer owner",  # Only used on the loopback emulator.
        "Content-Type": "application/json",
    })
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            payload = response.read()
            return response.status, json.loads(payload) if payload else None
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()


class ProfileRulesTest(unittest.TestCase):
    def setUp(self):
        status, _ = request("PUT", "", {"users": {
            "alice": {"profile": {"displayName": "Alice", "updatedAt": 1}},
            "bob": {"profile": {"displayName": "Bob", "updatedAt": 1}},
        }})
        self.assertEqual(200, status)

    def denied(self, method, path, data=None, identity="alice"):
        status, _ = request(method, path, data, identity)
        self.assertIn(status, (401, 403))

    def test_owner_reads_own_profile(self):
        status, profile = request("GET", "users/alice/profile", identity="alice")
        self.assertEqual(200, status)
        self.assertEqual("Alice", profile["displayName"])

    def test_owner_creates_profile_with_server_timestamp(self):
        status, _ = request("PUT", "users/charlie/profile",
                            {"displayName": "Charlie", "updatedAt": SERVER_TIME}, "charlie")
        self.assertEqual(200, status)

    def test_owner_updates_name_and_server_timestamp(self):
        status, _ = request("PATCH", "users/alice/profile",
                            {"displayName": "New name", "updatedAt": SERVER_TIME}, "alice")
        self.assertEqual(200, status)
        self.assertEqual("New name", request("GET", "users/alice/profile", identity="alice")[1]["displayName"])

    def test_unsigned_read_denied(self):
        self.denied("GET", "users/alice/profile", identity=None)

    def test_unsigned_write_denied(self):
        self.denied("PUT", "users/alice/profile",
                    {"displayName": "Anonymous", "updatedAt": SERVER_TIME}, None)

    def test_cross_account_read_denied(self):
        self.denied("GET", "users/bob/profile")

    def test_cross_account_write_denied(self):
        self.denied("PATCH", "users/bob/profile",
                    {"displayName": "Not Bob", "updatedAt": SERVER_TIME})

    def test_root_and_users_reads_denied(self):
        self.denied("GET", "")
        self.denied("GET", "users")
        self.denied("GET", "users/alice")

    def test_unknown_fields_denied(self):
        self.denied("PATCH", "users/alice/profile", {"role": "admin", "updatedAt": SERVER_TIME})

    def test_client_timestamp_denied(self):
        self.denied("PATCH", "users/alice/profile", {"displayName": "New name", "updatedAt": 123})

    def test_empty_name_denied(self):
        self.denied("PATCH", "users/alice/profile", {"displayName": "", "updatedAt": SERVER_TIME})

    def test_oversized_name_denied(self):
        self.denied("PATCH", "users/alice/profile", {"displayName": "a" * 61, "updatedAt": SERVER_TIME})

    def test_non_string_name_denied(self):
        self.denied("PATCH", "users/alice/profile", {"displayName": 42, "updatedAt": SERVER_TIME})

    def test_profile_deletion_denied(self):
        self.denied("DELETE", "users/alice/profile")

    def test_required_field_deletion_denied(self):
        self.denied("DELETE", "users/alice/profile/displayName")

    def test_other_app_paths_denied(self):
        self.denied("PUT", "tasks/one", {"title": "Outside BTVN03"})


def main():
    global BASE_URL
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--emulator-jar", type=Path)
    options = parser.parse_args()
    jars = sorted((Path.home() / ".cache/firebase/emulators").glob("firebase-database-emulator-*.jar"))
    jar = options.emulator_jar or (jars[-1] if jars else None)
    java = shutil.which("java")
    if not java or not jar or not jar.is_file():
        parser.error("Requires Java and RTDB emulator JAR; use --emulator-jar PATH if needed.")
    with socket.socket() as port_socket:
        port_socket.bind(("127.0.0.1", 0))
        port = port_socket.getsockname()[1]
    BASE_URL = f"http://127.0.0.1:{port}"
    scratch = ROOT / ".gradle/btvn03-check"
    scratch.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="rules-", dir=scratch) as directory:
        with (Path(directory) / "emulator.log").open("w", encoding="utf-8") as log:
            process = subprocess.Popen(
                [java, "-jar", str(jar.resolve()), "--host", "127.0.0.1", "--port", str(port)],
                cwd=directory, stdout=log, stderr=subprocess.STDOUT,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
            )
            try:
                deadline = time.monotonic() + 30
                while True:
                    if process.poll() is not None:
                        log.flush()
                        detail = (Path(directory) / "emulator.log").read_text(encoding="utf-8", errors="replace")
                        raise RuntimeError("Database emulator exited before startup:\n" + detail[-4000:])
                    try:
                        if request("GET", "")[0] == 200:
                            break
                    except (OSError, urllib.error.URLError):
                        pass
                    if time.monotonic() > deadline:
                        raise TimeoutError("Local database emulator startup timed out.")
                    time.sleep(0.2)
                rules = json.loads((ROOT / "firebase/database.rules.json").read_text(encoding="utf-8"))
                status, result = request("PUT", ".settings/rules", rules)
                if status != 200:
                    raise RuntimeError(f"Rules rejected by emulator: {status}: {result}")
                result = unittest.TextTestRunner(verbosity=2).run(
                    unittest.defaultTestLoader.loadTestsFromTestCase(ProfileRulesTest)
                )
                return 0 if result.wasSuccessful() else 1
            finally:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)


if __name__ == "__main__":
    raise SystemExit(main())
