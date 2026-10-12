import hmac
import os
import shutil
import subprocess
import time
from pathlib import Path
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse

APP_TOKEN = os.environ.get("ZERO_MANAGER_TOKEN", "")
COMPOSE_DIR = Path("/opt/zero")
START_TIME = time.monotonic()
app = FastAPI(title="ZERO VPS Manager Agent", docs_url=None, redoc_url=None, openapi_url=None)

def authorize(authorization: str | None):
    if not APP_TOKEN or not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    supplied = authorization[7:].strip()
    if not hmac.compare_digest(supplied, APP_TOKEN):
        raise HTTPException(status_code=401, detail="Unauthorized")

def command(args, timeout=8):
    # All callers below use fixed, allowlisted argument arrays; never user-provided commands.
    try:
        p = subprocess.run(args, capture_output=True, text=True, timeout=timeout, check=False)
    except subprocess.TimeoutExpired:
        raise HTTPException(status_code=504, detail="Command timed out")
    if p.returncode != 0:
        raise HTTPException(status_code=500, detail=(p.stderr.strip() or "Command failed")[:500])
    return p.stdout.strip()

@app.middleware("http")
async def auth_middleware(request: Request, call_next):
    if request.url.path == "/v1/healthz":
        return await call_next(request)
    try:
        authorize(request.headers.get("authorization"))
    except HTTPException as e:
        return JSONResponse(status_code=e.status_code, content={"detail": e.detail})
    return await call_next(request)

@app.get("/v1/healthz")
def healthz():
    return {"status": "ok", "service": "zero-vps-manager"}

@app.get("/v1/health")
def health():
    uptime = command(["uptime", "-p"])
    load = Path("/proc/loadavg").read_text().split()[:3]
    mem = {}
    for line in Path("/proc/meminfo").read_text().splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            mem[k] = int(v.strip().split()[0])
    total = mem.get("MemTotal", 0)
    available = mem.get("MemAvailable", 0)
    used_pct = round((total - available) / total * 100) if total else 0
    disk = shutil.disk_usage("/")
    disk_pct = round(disk.used / disk.total * 100) if disk.total else 0
    return {
        "status": "ok",
        "uptime": uptime,
        "load": " / ".join(load),
        "memory": f"{used_pct}% ({round((total-available)/1024)} / {round(total/1024)} MiB)",
        "disk": f"{disk_pct}% ({round(disk.used/1024**3,1)} / {round(disk.total/1024**3,1)} GiB)",
    }

@app.get("/v1/docker/services")
def docker_services():
    if not COMPOSE_DIR.joinpath("compose.yaml").exists() and not COMPOSE_DIR.joinpath("docker-compose.yml").exists():
        raise HTTPException(status_code=404, detail="Compose file not found in /opt/zero")
    out = command(["docker", "compose", "ps", "--format", "json"], timeout=12)
    import json
    services = []
    # docker compose versions may return one JSON object per line.
    for line in out.splitlines():
        try:
            item = json.loads(line)
            services.append({
                "name": str(item.get("Name", ""))[:100],
                "state": str(item.get("State", ""))[:40],
                "status": str(item.get("Status", ""))[:120],
            })
        except json.JSONDecodeError:
            continue
    return {"services": services}

@app.get("/v1/logs")
def logs():
    out = command(["journalctl", "-u", "zero-vps-manager", "-n", "60", "--no-pager"], timeout=8)
    return {"logs": out[-12000:]}

@app.post("/v1/actions/restart-api")
def restart_api():
    if not COMPOSE_DIR.joinpath("compose.yaml").exists() and not COMPOSE_DIR.joinpath("docker-compose.yml").exists():
        raise HTTPException(status_code=404, detail="Compose file not found in /opt/zero")
    # Deliberately fixed: never restart DB, remove volumes, or execute caller-supplied commands.
    out = command(["docker", "compose", "restart", "api"], timeout=45)
    return {"status": "ok", "message": "ZERO API restart command completed", "output": out[:1000]}
