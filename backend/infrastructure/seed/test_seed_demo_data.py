"""Unit tests for seed_demo_data using an in-memory fake of the public API."""
from __future__ import annotations

import json
import re
import unittest
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict, List, Optional

from seed_demo_data import (
    DemoSeeder,
    SeedConfig,
    SeedError,
    demo_reading_amounts,
    reading_uuid,
)

PASSWORD = "Demo-passw0rd-test"
FIXED_NOW = datetime(2030, 1, 1, tzinfo=timezone.utc)


def base_env(**overrides: str) -> Dict[str, str]:
    env = {"SEED_DEMO_PASSWORD": PASSWORD}
    env.update(overrides)
    return env


class FakeMarketplace:
    """Stateful stand-in for the gateway: enforces the rules the seed relies on."""

    def __init__(self) -> None:
        self.users: Dict[str, Dict[str, Any]] = {}
        self.roles: Dict[str, set] = {}
        self.devices: Dict[str, Dict[str, Any]] = {}
        self.readings: set = set()
        self.offers: Dict[str, Dict[str, Any]] = {}
        self.transactions: Dict[str, Dict[str, Any]] = {}
        self.idempotency: Dict[str, str] = {}
        self.calls: List[str] = []
        self.seen_credentials: List[str] = []
        self.admin_id = self._add_user("demo-admin", ["ADMIN"])

    def _add_user(self, username: str, roles: List[str]) -> str:
        user_id = str(uuid.uuid4())
        self.users[username] = {"id": user_id, "username": username, "password": PASSWORD}
        self.roles[user_id] = set(roles or ["NORMAL_USER"])
        return user_id

    def _user_by_token(self, token: Optional[str]) -> Optional[str]:
        if not token or not token.startswith("token-"):
            return None
        return token[len("token-"):]

    def request(self, method: str, path: str, body: Any = None, token: Optional[str] = None,
                headers: Optional[Dict[str, str]] = None):
        self.calls.append(f"{method} {path}")
        headers = headers or {}
        path_only = path.split("?")[0]
        query = path.split("?")[1] if "?" in path else ""

        if method == "POST" and path_only == "/api/v1/auth/login":
            user = self.users.get(body["identifier"])
            if not user or user["password"] != body["password"]:
                return 401, {"error": "invalid"}
            return 200, {"accessToken": f"token-{user['id']}", "tokenType": "Bearer"}

        if method == "POST" and path_only == "/api/v1/auth/register":
            if body["username"] in self.users:
                return 409, {"error": "conflict"}
            self._add_user(body["username"], [])
            return 201, {"username": body["username"]}

        if method == "GET" and path_only == "/api/v1/admin/users":
            items = [{"id": u["id"], "username": u["username"]} for u in self.users.values()]
            return 200, {"items": items, "page": 0, "size": 50, "hasNext": False}

        if method == "POST" and re.fullmatch(r"/api/v1/admin/users/[^/]+/roles", path_only):
            user_id = path_only.split("/")[5]
            self.roles.setdefault(user_id, set()).add(body["role"])
            return 200, {}

        caller = self._user_by_token(token)
        is_device_reading = re.fullmatch(r"/api/v1/market/devices/[^/]+/readings", path_only) is not None
        if caller is None and not is_device_reading:
            return 401, {}

        if method == "POST" and path_only == "/api/v1/market/devices":
            if "ADMIN" not in self.roles[self.admin_id]:
                return 403, {}
            identifier = body["deviceIdentifier"]
            if identifier in self.devices:
                return 409, {"error": "conflict"}
            device_id = str(uuid.uuid4())
            credential = f"cred-{device_id}"
            self.devices[identifier] = {"id": device_id, "credential": credential,
                                        "owner": body["ownerUserId"]}
            self.seen_credentials.append(credential)
            return 201, {"device": {"id": device_id, "ownerUserId": body["ownerUserId"],
                                    "deviceIdentifier": identifier}, "credential": credential}

        if method == "POST" and re.fullmatch(r"/api/v1/market/devices/[^/]+/readings", path_only):
            device_id = path_only.split("/")[5]
            if headers.get("X-Device-Credential") != f"cred-{device_id}":
                return 401, {}
            reading_id = body["id"]
            if reading_id in self.readings:
                return 409, {}
            if Decimal(body["surplusEnergyKwh"]) > Decimal(body["generatedEnergyKwh"]):
                return 400, {}
            self.readings.add(reading_id)
            return 201, {}

        if method == "GET" and path_only == "/api/v1/offers/me":
            status = None
            if "status=" in query:
                status = query.split("status=")[1].split("&")[0]
            content = [
                {"id": oid, "status": o["status"]}
                for oid, o in self.offers.items()
                if o["seller"] == caller and (status is None or o["status"] == status)
            ]
            return 200, {"content": content, "page": 0, "size": 100,
                         "totalElements": len(content), "totalPages": 1}

        if method == "POST" and path_only == "/api/v1/offers":
            offer_id = str(uuid.uuid4())
            self.offers[offer_id] = {"seller": caller, "status": "ACTIVE",
                                     "amount": body["energyAmountKwh"], "price": body["pricePerKwh"]}
            return 201, {"id": offer_id, "status": "ACTIVE"}

        if method == "DELETE" and path_only.startswith("/api/v1/offers/"):
            offer_id = path_only.rsplit("/", 1)[1]
            offer = self.offers.get(offer_id)
            if not offer or offer["seller"] != caller or offer["status"] != "ACTIVE":
                return 409, {}
            offer["status"] = "CANCELLED"
            return 204, None

        if method == "GET" and path_only == "/api/v1/transactions/me":
            items = [{"id": tid} for tid, t in self.transactions.items()
                     if caller in (t["buyer"], t["seller"])]
            return 200, items

        if method == "POST" and path_only == "/api/v1/transactions":
            key = headers.get("Idempotency-Key")
            offer_id = body["offerId"]
            if key and key in self.idempotency:
                return 200, {"id": self.idempotency[key]}
            offer = self.offers.get(offer_id)
            if not offer or offer["status"] != "ACTIVE" or offer["seller"] == caller:
                return 409, {}
            offer["status"] = "SOLD"
            tx_id = str(uuid.uuid4())
            self.transactions[tx_id] = {"buyer": caller, "seller": offer["seller"]}
            if key:
                self.idempotency[key] = tx_id
            return 201, {"id": tx_id, "status": "CONFIRMED"}

        return 404, {}


def make_config(**overrides: str) -> SeedConfig:
    return SeedConfig.from_env(base_env(**overrides))


def run_seed(marketplace: FakeMarketplace, config: SeedConfig) -> Dict[str, int]:
    logs: List[str] = []
    seeder = DemoSeeder(marketplace, config, log=logs.append,
                        clock=lambda: FIXED_NOW, sleep=lambda _: None)
    counts = seeder.run()
    run_seed.logs = logs  # type: ignore[attr-defined]
    return counts


class SeedConfigTest(unittest.TestCase):
    def test_defaults_match_requested_quantities(self) -> None:
        config = SeedConfig.from_env(base_env())
        self.assertEqual(config.normal_users, 8)
        self.assertEqual(config.devices, 4)
        self.assertEqual(config.devices * config.readings_per_device, 40)
        self.assertEqual(config.offers_total, 20)
        self.assertEqual(config.transactions, 10)

    def test_missing_password_is_rejected(self) -> None:
        with self.assertRaises(SeedError):
            SeedConfig.from_env({})

    def test_non_integer_count_is_rejected(self) -> None:
        with self.assertRaises(SeedError):
            SeedConfig.from_env(base_env(SEED_DEVICES="four"))

    def test_transactions_cannot_exceed_non_cancelled_offers(self) -> None:
        with self.assertRaises(SeedError):
            make_config(SEED_OFFERS_TOTAL="5", SEED_CANCELLED_OFFERS="2", SEED_TRANSACTIONS="4")

    def test_at_least_two_normal_users_are_required(self) -> None:
        with self.assertRaises(SeedError):
            make_config(SEED_NORMAL_USERS="1")


class DeterministicValuesTest(unittest.TestCase):
    def test_reading_ids_are_stable_and_unique_per_index(self) -> None:
        self.assertEqual(reading_uuid("dev-a", 3), reading_uuid("dev-a", 3))
        self.assertNotEqual(reading_uuid("dev-a", 3), reading_uuid("dev-a", 4))
        self.assertNotEqual(reading_uuid("dev-a", 3), reading_uuid("dev-b", 3))

    def test_surplus_never_exceeds_generated(self) -> None:
        for index in range(40):
            generated, surplus = demo_reading_amounts(index)
            self.assertLessEqual(surplus, generated)
            self.assertGreater(surplus, Decimal("0"))
            self.assertEqual(generated.as_tuple().exponent, -3)


class DemoSeederTest(unittest.TestCase):
    def test_first_run_creates_the_requested_quantities(self) -> None:
        marketplace = FakeMarketplace()
        counts = run_seed(marketplace, make_config())

        self.assertEqual(counts["users_created"], 9)  # 8 normal users + 1 auditor
        self.assertEqual(counts["devices_created"], 4)
        self.assertEqual(counts["readings_created"], 40)
        self.assertEqual(counts["offers_created"], 20)
        self.assertEqual(counts["offers_cancelled"], 2)
        self.assertEqual(counts["transactions_created"], 10)
        self.assertEqual(len(marketplace.readings), 40)

    def test_auditor_receives_security_auditor_role(self) -> None:
        marketplace = FakeMarketplace()
        run_seed(marketplace, make_config())
        auditor_id = marketplace.users["demo-auditor"]["id"]
        self.assertIn("SECURITY_AUDITOR", marketplace.roles[auditor_id])

    def test_second_run_creates_nothing(self) -> None:
        marketplace = FakeMarketplace()
        run_seed(marketplace, make_config())
        before = (len(marketplace.users), len(marketplace.devices), len(marketplace.readings),
                  len(marketplace.offers), len(marketplace.transactions))

        counts = run_seed(marketplace, make_config())

        after = (len(marketplace.users), len(marketplace.devices), len(marketplace.readings),
                 len(marketplace.offers), len(marketplace.transactions))
        self.assertEqual(before, after)
        self.assertEqual(counts["users_created"], 0)
        self.assertEqual(counts["devices_created"], 0)
        self.assertEqual(counts["offers_created"], 0)
        self.assertEqual(counts["transactions_created"], 0)

    def test_transactions_respect_buyer_seller_relationship(self) -> None:
        marketplace = FakeMarketplace()
        run_seed(marketplace, make_config())
        for transaction in marketplace.transactions.values():
            self.assertNotEqual(transaction["buyer"], transaction["seller"])

    def test_output_never_contains_passwords_or_device_credentials(self) -> None:
        marketplace = FakeMarketplace()
        run_seed(marketplace, make_config())
        logged = "\n".join(run_seed.logs)  # type: ignore[attr-defined]
        self.assertNotIn(PASSWORD, logged)
        for credential in marketplace.seen_credentials:
            self.assertNotIn(credential, logged)

    def test_failed_login_stops_with_generic_message(self) -> None:
        marketplace = FakeMarketplace()
        marketplace.users["demo-admin"]["password"] = "other-password-value"
        config = make_config()
        seeder = DemoSeeder(marketplace, config, log=lambda _: None,
                            clock=lambda: FIXED_NOW, sleep=lambda _: None)
        with self.assertRaises(SeedError) as context:
            seeder.run()
        self.assertNotIn(PASSWORD, str(context.exception))
        self.assertIn("administrator login", str(context.exception))


if __name__ == "__main__":
    unittest.main()
