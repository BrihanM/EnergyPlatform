#!/usr/bin/env python3
"""Idempotent development seed for the EnergyProsumer testbed.

Drives the public HTTP API exposed by the API gateway, so every record is created
through the same use cases and business rules as a real client. It only uses the
Python standard library. It is intended for development and demonstrations only.

Re-running the script never duplicates data: existing users, devices, readings,
offers and transactions are detected and skipped. Device credentials are kept in
memory for the current run and are never printed or logged.

The first ADMIN cannot be created over HTTP. The auth-service
``DevSeedAdminBootstrap`` component creates that account in-process when
``DEV_SEED_ENABLED=true``; this script expects it to exist.
"""
from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from decimal import ROUND_HALF_UP, Decimal
from typing import Any, Callable, Dict, List, Mapping, Optional, Tuple

SEED_NAMESPACE = uuid.UUID("6f1c7a52-3d1e-5b9a-8c2e-0f4d9a7b1e33")
DEVICE_CREDENTIAL_HEADER = "X-Device-Credential"
SECURITY_AUDITOR_ROLE = "SECURITY_AUDITOR"
FIRST_NAMES = ["Ana", "Luis", "Marta", "Pablo", "Lucía", "Diego", "Elena", "Javier"]
LAST_NAMES = ["García", "Martín", "López", "Sánchez", "Pérez", "Ruiz", "Gómez", "Díaz"]


class SeedError(RuntimeError):
    """Raised when the seed cannot continue; the message never contains secrets."""


@dataclass(frozen=True)
class SeedConfig:
    base_url: str
    demo_password: str
    admin_username: str
    auditor_username: str
    normal_users: int
    devices: int
    readings_per_device: int
    offers_total: int
    cancelled_offers: int
    transactions: int

    @classmethod
    def from_env(cls, env: Mapping[str, str]) -> "SeedConfig":
        config = cls(
            base_url=env.get("SEED_BASE_URL", "http://api-gateway:8080").rstrip("/"),
            demo_password=_required(env, "SEED_DEMO_PASSWORD"),
            admin_username=env.get("SEED_DEMO_ADMIN_USERNAME", "demo-admin"),
            auditor_username=env.get("SEED_AUDITOR_USERNAME", "demo-auditor"),
            normal_users=_int(env, "SEED_NORMAL_USERS", 8),
            devices=_int(env, "SEED_DEVICES", 4),
            readings_per_device=_int(env, "SEED_READINGS_PER_DEVICE", 10),
            offers_total=_int(env, "SEED_OFFERS_TOTAL", 20),
            cancelled_offers=_int(env, "SEED_CANCELLED_OFFERS", 2),
            transactions=_int(env, "SEED_TRANSACTIONS", 10),
        )
        config.validate()
        return config

    def validate(self) -> None:
        if self.normal_users < 2:
            raise SeedError("SEED_NORMAL_USERS must be at least 2 so buyers differ from sellers")
        if self.devices < 1 or self.readings_per_device < 1:
            raise SeedError("SEED_DEVICES and SEED_READINGS_PER_DEVICE must be at least 1")
        if not 0 <= self.cancelled_offers <= self.offers_total:
            raise SeedError("SEED_CANCELLED_OFFERS must be between 0 and SEED_OFFERS_TOTAL")
        if not 0 <= self.transactions <= self.offers_total - self.cancelled_offers:
            raise SeedError("SEED_TRANSACTIONS must fit in the offers that are not cancelled")


def _required(env: Mapping[str, str], name: str) -> str:
    value = env.get(name, "")
    if not value:
        raise SeedError(f"{name} must be set")
    return value


def _int(env: Mapping[str, str], name: str, default: int) -> int:
    raw = env.get(name, "")
    if raw == "":
        return default
    try:
        return int(raw)
    except ValueError as error:
        raise SeedError(f"{name} must be an integer") from error


class HttpApi:
    """Minimal JSON client returning ``(status, body)`` without raising on HTTP errors."""

    def __init__(self, base_url: str, timeout: float = 15.0) -> None:
        self.base_url = base_url
        self.timeout = timeout

    def request(self, method: str, path: str, body: Any = None, token: Optional[str] = None,
                headers: Optional[Dict[str, str]] = None) -> Tuple[int, Any]:
        data = json.dumps(body).encode("utf-8") if body is not None else None
        request = urllib.request.Request(self.base_url + path, data=data, method=method)
        request.add_header("Accept", "application/json")
        if body is not None:
            request.add_header("Content-Type", "application/json")
        if token:
            request.add_header("Authorization", f"Bearer {token}")
        for name, value in (headers or {}).items():
            request.add_header(name, value)
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                return response.status, _parse_json(response.read())
        except urllib.error.HTTPError as error:
            return error.code, _parse_json(error.read())


def _parse_json(raw: bytes) -> Any:
    if not raw:
        return None
    try:
        return json.loads(raw)
    except ValueError:
        return None


class DemoSeeder:
    """Creates demo data through the public API; each step is skipped when already present."""

    def __init__(self, api: Any, config: SeedConfig, log: Callable[[str], None] = print,
                 clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
                 sleep: Callable[[float], None] = time.sleep) -> None:
        self.api = api
        self.config = config
        self.log = log
        self.clock = clock
        self.sleep = sleep
        self.counts: Dict[str, int] = {
            "users_created": 0, "users_existing": 0,
            "devices_created": 0, "devices_existing": 0,
            "readings_created": 0, "readings_existing": 0,
            "offers_created": 0, "offers_cancelled": 0,
            "transactions_created": 0,
        }

    def run(self) -> Dict[str, int]:
        admin_token = self._login_with_retry(self.config.admin_username, self.config.demo_password)
        self.log("[seed] administrator login ok")

        normal_usernames = [self._normal_username(i) for i in range(self.config.normal_users)]
        for index, username in enumerate(normal_usernames):
            self._register(username, FIRST_NAMES[index % len(FIRST_NAMES)],
                           LAST_NAMES[index % len(LAST_NAMES)])
        self._register(self.config.auditor_username, "Demo", "Auditor")

        user_ids = self._user_ids(admin_token)
        self._ensure_auditor_role(admin_token, user_ids[self.config.auditor_username])

        tokens = {name: self._login(name, self.config.demo_password) for name in normal_usernames}

        self._seed_devices_and_readings(admin_token, user_ids, normal_usernames)
        self._seed_offers(tokens, normal_usernames)
        self._seed_transactions(tokens, normal_usernames)

        self.log("[seed] done: " + ", ".join(f"{key}={value}" for key, value in self.counts.items()))
        return dict(self.counts)

    @staticmethod
    def _normal_username(index: int) -> str:
        return f"demo-user-{index + 1:02d}"

    def _login(self, username: str, password: str) -> str:
        status, body = self.api.request("POST", "/api/v1/auth/login",
                                        {"identifier": username, "password": password})
        if status != 200 or not isinstance(body, dict) or "accessToken" not in body:
            raise SeedError(f"login failed for demo account (HTTP {status})")
        return body["accessToken"]

    def _login_with_retry(self, username: str, password: str, attempts: int = 60) -> str:
        """Waits for auth-service readiness and for the dev admin bootstrap to finish."""
        last_status: Optional[int] = None
        for _ in range(attempts):
            try:
                status, body = self.api.request("POST", "/api/v1/auth/login",
                                                {"identifier": username, "password": password})
            except OSError:
                status, body = 0, None
            if status == 200 and isinstance(body, dict) and "accessToken" in body:
                return body["accessToken"]
            last_status = status
            self.sleep(2)
        raise SeedError(f"administrator login did not succeed (last HTTP {last_status}); "
                        "check DEV_SEED_ENABLED and SEED_DEMO_PASSWORD on auth-service")

    def _register(self, username: str, first_name: str, last_name: str) -> None:
        status, _ = self.api.request("POST", "/api/v1/auth/register", {
            "firstName": first_name,
            "lastName": last_name,
            "username": username,
            "email": f"{username}@example.test",
            "password": self.config.demo_password,
        })
        if status in (200, 201):
            self.counts["users_created"] += 1
        elif status == 409:
            self.counts["users_existing"] += 1
        else:
            raise SeedError(f"registration failed for {username} (HTTP {status})")

    def _user_ids(self, admin_token: str) -> Dict[str, str]:
        ids: Dict[str, str] = {}
        page = 0
        while True:
            status, body = self.api.request("GET", f"/api/v1/admin/users?page={page}&size=50",
                                            token=admin_token)
            if status != 200 or not isinstance(body, dict):
                raise SeedError(f"could not list users (HTTP {status})")
            for item in body.get("items", []):
                ids[item["username"]] = item["id"]
            if not body.get("hasNext"):
                break
            page += 1
        return ids

    def _ensure_auditor_role(self, admin_token: str, auditor_id: str) -> None:
        status, _ = self.api.request("POST", f"/api/v1/admin/users/{auditor_id}/roles",
                                     {"role": SECURITY_AUDITOR_ROLE}, token=admin_token)
        if status not in (200, 201, 204, 409):
            raise SeedError(f"could not assign {SECURITY_AUDITOR_ROLE} (HTTP {status})")

    def _seed_devices_and_readings(self, admin_token: str, user_ids: Dict[str, str],
                                   normal_usernames: List[str]) -> None:
        for index in range(self.config.devices):
            owner_username = normal_usernames[index % len(normal_usernames)]
            identifier = f"demo-meter-{index + 1:02d}"
            status, body = self.api.request("POST", "/api/v1/market/devices", {
                "ownerUserId": user_ids[owner_username],
                "deviceIdentifier": identifier,
            }, token=admin_token)
            if status == 409:
                self.counts["devices_existing"] += 1
                continue
            if status != 201 or not isinstance(body, dict):
                raise SeedError(f"device provisioning failed for {identifier} (HTTP {status})")
            self.counts["devices_created"] += 1
            # The credential is returned once and used only for this run's readings.
            self._ingest_readings(body["device"]["id"], body["credential"])

    def _ingest_readings(self, device_id: str, credential: str) -> None:
        now = self.clock()
        for reading_index in range(self.config.readings_per_device):
            generated, surplus = demo_reading_amounts(reading_index)
            measured_at = now - timedelta(hours=self.config.readings_per_device - reading_index)
            status, _ = self.api.request(
                "POST", f"/api/v1/market/devices/{device_id}/readings", {
                    "id": str(reading_uuid(device_id, reading_index)),
                    "generatedEnergyKwh": str(generated),
                    "surplusEnergyKwh": str(surplus),
                    "measuredAt": _iso(measured_at),
                }, headers={DEVICE_CREDENTIAL_HEADER: credential})
            if status == 201:
                self.counts["readings_created"] += 1
            elif status == 409:
                self.counts["readings_existing"] += 1
            else:
                raise SeedError(f"reading ingestion failed (HTTP {status})")

    def _seed_offers(self, tokens: Dict[str, str], usernames: List[str]) -> None:
        offers = self._load_offers(tokens, usernames)
        cancelled_now = sum(1 for offer in offers if offer["status"] == "CANCELLED")
        for _ in range(max(0, self.config.offers_total - len(offers))):
            seller_index = len(offers) % len(usernames)
            offer_id = self._create_offer(tokens[usernames[seller_index]], len(offers))
            offers.append({"id": offer_id, "seller": seller_index, "status": "ACTIVE"})
            self.counts["offers_created"] += 1

        for offer in offers:
            if cancelled_now >= self.config.cancelled_offers:
                break
            if offer["status"] != "ACTIVE":
                continue
            self._cancel_offer(tokens[usernames[offer["seller"]]], offer["id"])
            offer["status"] = "CANCELLED"
            cancelled_now += 1
            self.counts["offers_cancelled"] += 1

    def _load_offers(self, tokens: Dict[str, str], usernames: List[str]) -> List[Dict[str, Any]]:
        offers: List[Dict[str, Any]] = []
        for seller_index, username in enumerate(usernames):
            status, body = self.api.request("GET", "/api/v1/offers/me?size=100", token=tokens[username])
            if status != 200 or not isinstance(body, dict):
                raise SeedError(f"could not list offers for {username} (HTTP {status})")
            for item in body.get("content", []):
                offers.append({"id": item["id"], "seller": seller_index, "status": item["status"]})
        return offers

    def _create_offer(self, token: str, offer_index: int) -> str:
        expires_at = self.clock() + timedelta(days=7)
        status, body = self.api.request("POST", "/api/v1/offers", {
            "energyAmountKwh": str(Decimal("2.500") + Decimal(offer_index) * Decimal("0.500")),
            "pricePerKwh": str(Decimal("0.15") + Decimal(offer_index % 5) * Decimal("0.01")),
            "expiresAt": _iso(expires_at),
        }, token=token)
        if status != 201 or not isinstance(body, dict):
            raise SeedError(f"offer creation failed (HTTP {status})")
        return body["id"]

    def _cancel_offer(self, token: str, offer_id: str) -> None:
        status, _ = self.api.request("DELETE", f"/api/v1/offers/{offer_id}", token=token)
        if status not in (200, 204):
            raise SeedError(f"offer cancellation failed (HTTP {status})")

    def _seed_transactions(self, tokens: Dict[str, str], usernames: List[str]) -> None:
        needed = self.config.transactions - len(self._existing_transaction_ids(tokens))
        if needed <= 0:
            return
        for offer in self._load_offers(tokens, usernames):
            if needed <= 0:
                break
            if offer["status"] != "ACTIVE":
                continue
            # The next user is always a different account because at least two sellers exist.
            buyer_index = (offer["seller"] + 1) % len(usernames)
            self._purchase(tokens[usernames[buyer_index]], offer["id"])
            needed -= 1
            self.counts["transactions_created"] += 1

    def _existing_transaction_ids(self, tokens: Dict[str, str]) -> set:
        ids = set()
        for token in tokens.values():
            status, body = self.api.request("GET", "/api/v1/transactions/me?size=100&offset=0", token=token)
            if status != 200 or not isinstance(body, list):
                raise SeedError(f"could not list transactions (HTTP {status})")
            ids.update(item["id"] for item in body)
        return ids

    def _purchase(self, token: str, offer_id: str) -> None:
        status, _ = self.api.request("POST", "/api/v1/transactions", {"offerId": offer_id},
                                     token=token,
                                     headers={"Idempotency-Key": f"seed-tx-{offer_id}"})
        if status not in (200, 201):
            raise SeedError(f"purchase failed (HTTP {status})")


def demo_reading_amounts(reading_index: int) -> Tuple[Decimal, Decimal]:
    """Returns generated and surplus kWh; surplus never exceeds generated so the reading stays VALID."""
    generated = (Decimal("1.500") + Decimal(reading_index) * Decimal("0.250")).quantize(
        Decimal("0.001"), rounding=ROUND_HALF_UP)
    surplus = (generated * Decimal("0.35")).quantize(Decimal("0.001"), rounding=ROUND_HALF_UP)
    return generated, surplus


def reading_uuid(device_id: str, reading_index: int) -> uuid.UUID:
    return uuid.uuid5(SEED_NAMESPACE, f"reading:{device_id}:{reading_index}")


def _iso(moment: datetime) -> str:
    return moment.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")[:-4] + "Z"


def main() -> int:
    try:
        config = SeedConfig.from_env(os.environ)
        DemoSeeder(HttpApi(config.base_url), config).run()
    except SeedError as error:
        print(f"[seed] FAILED: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
