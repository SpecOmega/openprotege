#!/usr/bin/env python3
"""Run a disposable, end-to-end local OpenProtege workflow using only Python's standard library."""

import argparse
import http.cookiejar
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path


class ApiError(RuntimeError):
    pass


class Client:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(self.cookies)
        )
        self.csrf = None

    def request(self, method, path, *, payload=None, upload=None, expected=(200,)):
        headers = {"Accept": "application/json"}
        body = None
        if method in {"POST", "PUT", "PATCH", "DELETE"}:
            if self.csrf is None:
                self.refresh_csrf()
            headers["X-XSRF-TOKEN"] = self.csrf

        if upload is not None:
            file_name, content = upload
            boundary = "OpenProtege" + uuid.uuid4().hex
            headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
            body = (
                f"--{boundary}\r\n"
                f'Content-Disposition: form-data; name="file"; filename="{file_name}"\r\n'
                "Content-Type: text/turtle\r\n\r\n"
            ).encode() + content + f"\r\n--{boundary}--\r\n".encode()
        elif payload is not None:
            headers["Content-Type"] = "application/json"
            body = json.dumps(payload).encode()

        request = urllib.request.Request(
            self.base_url + path, data=body, headers=headers, method=method
        )
        try:
            with self.opener.open(request, timeout=60) as response:
                status = response.status
                text = response.read().decode("utf-8")
        except urllib.error.HTTPError as error:
            status = error.code
            text = error.read().decode("utf-8", errors="replace")
        except (urllib.error.URLError, TimeoutError) as error:
            raise ApiError(f"{method} {path} failed to reach {self.base_url}: {error}") from error

        if status not in expected:
            raise ApiError(
                f"{method} {path}: expected HTTP {expected}, received {status}: {text}"
            )
        if not text:
            return None
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text

    def refresh_csrf(self):
        token = self.request("GET", "/api/auth/csrf")
        self.csrf = token["token"]

    def login(self, email, password):
        self.refresh_csrf()
        self.request(
            "POST",
            "/api/auth/login",
            payload={"email": email, "password": password},
            expected=(204,),
        )
        self.csrf = None
        self.refresh_csrf()


def require(condition, message):
    if not condition:
        raise ApiError(message)


def invite_account(admin, base_url, email, password):
    issued = admin.request(
        "POST", "/api/admin/invitations", payload={"email": email}, expected=(200,)
    )
    account = Client(base_url)
    account.request(
        "POST",
        "/api/auth/invitations/accept",
        payload={"token": issued["token"], "password": password},
        expected=(201,),
    )
    account.login(email, password)
    return account


def run(args):
    admin_email = os.getenv("ADMIN_BOOTSTRAP_EMAIL")
    admin_password = os.getenv("ADMIN_BOOTSTRAP_PASSWORD")
    demo_password = os.getenv("DEMO_MEMBER_PASSWORD")
    if not admin_email or not admin_password:
        raise ApiError(
            "Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD to the "
            "administrator of an already-started dev database."
        )
    if not demo_password or not 12 <= len(demo_password) <= 128:
        raise ApiError("Set DEMO_MEMBER_PASSWORD to a local-only password of 12 to 128 characters.")

    ontology_path = Path(args.ontology).resolve()
    ontology = ontology_path.read_bytes()
    run_id = f"{int(time.time())}-{uuid.uuid4().hex[:6]}"
    users = {
        "team_admin": (f"team-admin-{run_id}@example.test", demo_password),
        "editor": (f"editor-{run_id}@example.test", demo_password),
        "viewer": (f"viewer-{run_id}@example.test", demo_password),
        "team_only": (f"team-only-{run_id}@example.test", demo_password),
    }

    admin = Client(args.base_url)
    admin.login(admin_email, admin_password)
    for email, password in users.values():
        invite_account(admin, args.base_url, email, password)

    team = admin.request(
        "POST", "/api/teams", payload={"name": f"{args.team_name} {run_id}"}, expected=(201,)
    )
    team_id = team["id"]
    for name, role in (
        ("team_admin", "ADMIN"),
        ("editor", "MEMBER"),
        ("viewer", "MEMBER"),
        ("team_only", "MEMBER"),
    ):
        admin.request(
            "POST",
            f"/api/teams/{team_id}/members",
            payload={"email": users[name][0], "role": role},
            expected=(201,),
        )

    project = admin.request(
        "POST",
        "/api/projects",
        payload={
            "name": f"{args.project_name} {run_id}",
            "visibility": "PRIVATE",
            "teamId": team_id,
        },
        expected=(201,),
    )
    project_id = project["id"]
    require(project["role"] == "OWNER", "creator should own the team project")

    clients = {}
    for name, role in (("team_admin", "ADMIN"), ("editor", "EDITOR"), ("viewer", "VIEWER")):
        clients[name] = Client(args.base_url)
        clients[name].login(*users[name])
        admin.request(
            "POST",
            f"/api/projects/{project_id}/members",
            payload={"email": users[name][0], "role": role},
            expected=(201,),
        )

    team_admin = clients["team_admin"]
    editor = clients["editor"]
    viewer = clients["viewer"]
    outsider = Client(args.base_url)
    outsider.login(*users["team_only"])

    require(
        team_admin.request("GET", f"/api/teams/{team_id}/members") is not None,
        "team admin could not list team membership",
    )
    editor.request("GET", f"/api/projects/{project_id}/members", expected=(403,))
    viewer.request("GET", f"/api/projects/{project_id}/members", expected=(403,))
    outsider.request("GET", f"/api/projects/{project_id}", expected=(404,))

    imported = editor.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/import",
        upload=(ontology_path.name, ontology),
        expected=(201,),
    )
    version_id = imported["id"]
    require(imported["format"] == "Turtle", "uploaded fixture was not recognized as Turtle")
    editor.request(
        "PUT",
        f"/api/projects/{project_id}",
        payload={"name": "Editor must not update metadata", "visibility": "PUBLIC"},
        expected=(403,),
    )
    viewer.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/import",
        upload=(ontology_path.name, ontology),
        expected=(403,),
    )

    export = viewer.request(
        "GET",
        f"/api/projects/{project_id}/ontologies/versions/{version_id}/export?format=Turtle",
    )
    require(
        isinstance(export, str) and export.encode() == ontology,
        "viewer export did not return the original Turtle bytes",
    )
    reasoning = viewer.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/versions/{version_id}/reasoning/start",
        payload={"engine": "HERMIT"},
    )
    require(reasoning["consistent"] is True, "HermiT did not report a consistent ontology")
    require(reasoning["inOwl2DlProfile"] is True, "fixture is outside the OWL 2 DL profile")

    iris = args.iris.rstrip("#")
    classification = viewer.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/versions/{version_id}/reasoning/classify",
        payload={"individualIri": f"{iris}#{args.individual}"},
    )
    require(
        f"{iris}#Person" in classification["allTypeIris"],
        "HermiT did not infer that the student is a Person",
    )
    rule_result = viewer.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/versions/{version_id}/reasoning/rules/apply",
        payload={
            "ruleText": f"Rule: <{iris}#Student>(?x) -> <{iris}#ActiveStudent>(?x)",
            "individualIri": f"{iris}#{args.individual}",
        },
    )
    require(
        f"{iris}#ActiveStudent" in rule_result["inferredTypeIris"],
        "SWRL preview did not infer ActiveStudent",
    )

    metadata = team_admin.request(
        "PUT",
        f"/api/projects/{project_id}",
        payload={"name": f"{args.project_name} reviewed {run_id}", "visibility": "PRIVATE"},
    )
    require(metadata["role"] == "ADMIN", "team admin should have project ADMIN role")

    restored = editor.request(
        "POST",
        f"/api/projects/{project_id}/ontologies/versions/{version_id}/restore",
        payload={},
        expected=(201,),
    )
    require(restored["id"] != version_id, "restore must create an immutable new snapshot")
    restored_export = admin.request(
        "GET",
        f"/api/projects/{project_id}/ontologies/versions/{restored['id']}/export?format=Turtle",
    )
    require(
        isinstance(restored_export, str) and restored_export.encode() == ontology,
        "restored ontology bytes differ from the source version",
    )
    versions = admin.request("GET", f"/api/projects/{project_id}/ontologies/versions")
    require(versions["total"] == 2, "expected the imported and restored snapshots")

    print("PASS: developer scenario completed.")
    print(f"  team: {team_id}")
    print(f"  private team project: {project_id}")
    print(f"  source ontology version: {version_id}")
    print(f"  restored ontology version: {restored['id']}")
    print("  demo sign-ins (all use the DEMO_MEMBER_PASSWORD you supplied):")
    for name, role in (
        ("team_admin", "team ADMIN / project ADMIN"),
        ("editor", "team MEMBER / project EDITOR"),
        ("viewer", "team MEMBER / project VIEWER"),
        ("team_only", "team MEMBER / no project access"),
    ):
        print(f"    {role}: {users[name][0]}")
    print("  roles verified: team OWNER/ADMIN/MEMBER; project OWNER/ADMIN/EDITOR/VIEWER")
    print("  boundaries verified: team-only private-project access denied;")
    print("    EDITOR metadata/member administration denied; VIEWER import denied")
    print("  workflow verified: Turtle import/export, HermiT validation/classification,")
    print("    SWRL preview, immutable restore, original-byte comparison")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default=os.getenv("OPENPROTEGE_BASE_URL", "http://127.0.0.1:8080"))
    parser.add_argument("--ontology", required=True, help="Path to the language-specific Turtle fixture")
    parser.add_argument("--team-name", required=True)
    parser.add_argument("--project-name", required=True)
    parser.add_argument("--iris", required=True, help="Ontology namespace without trailing #")
    parser.add_argument("--individual", required=True, help="Local name of the sample individual")
    args = parser.parse_args()
    try:
        run(args)
    except (ApiError, KeyError, TypeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
