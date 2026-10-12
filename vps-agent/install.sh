#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Jalankan: sudo bash vps-agent/install.sh"
  exit 1
fi
command -v tailscale >/dev/null || { echo "Tailscale belum terpasang/aktif."; exit 1; }
command -v docker >/dev/null || { echo "Docker belum terpasang."; exit 1; }
[[ -d /opt/zero ]] || { echo "/opt/zero tidak ditemukan; berhenti tanpa perubahan."; exit 1; }

TS_IP="$(tailscale ip -4 | head -n1 | tr -d '[:space:]')"
[[ "$TS_IP" =~ ^100\. ]] || { echo "IP Tailscale IPv4 tidak terdeteksi. Tidak ada perubahan."; exit 1; }

# Preserve existing app and database. Manager is installed in its own directory.
install -d -m 0750 /opt/zero-vps-manager
install -m 0644 "$(dirname "$0")/agent.py" /opt/zero-vps-manager/agent.py
install -m 0644 "$(dirname "$0")/requirements.txt" /opt/zero-vps-manager/requirements.txt

if [[ ! -f /etc/zero-vps-manager.env ]]; then
  TOKEN="$(openssl rand -hex 32)"
  printf 'ZERO_MANAGER_TOKEN=%s\n' "$TOKEN" > /etc/zero-vps-manager.env
  chmod 600 /etc/zero-vps-manager.env
else
  chmod 600 /etc/zero-vps-manager.env
fi

python3 -m venv /opt/zero-vps-manager/venv
/opt/zero-vps-manager/venv/bin/pip install --disable-pip-version-check --no-cache-dir -r /opt/zero-vps-manager/requirements.txt

cat > /etc/systemd/system/zero-vps-manager.service <<EOF
[Unit]
Description=ZERO VPS Manager restricted agent
After=network-online.target tailscaled.service docker.service
Wants=network-online.target
Requires=tailscaled.service

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=/opt/zero-vps-manager
EnvironmentFile=/etc/zero-vps-manager.env
ExecStart=/opt/zero-vps-manager/venv/bin/uvicorn agent:app --host ${TS_IP} --port 8765 --workers 1
Restart=on-failure
RestartSec=3
UMask=0077
NoNewPrivileges=true
PrivateTmp=true
ProtectHome=true
ProtectSystem=full
ReadWritePaths=/opt/zero /var/log /run
RestrictAddressFamilies=AF_INET AF_INET6 AF_UNIX
LockPersonality=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now zero-vps-manager
sleep 2
if ! systemctl is-active --quiet zero-vps-manager; then
  systemctl status zero-vps-manager --no-pager || true
  journalctl -u zero-vps-manager -n 50 --no-pager || true
  echo "Service gagal start. /opt/zero dan database tidak disentuh."
  exit 1
fi

echo
echo "=== ZERO VPS MANAGER INSTALLED ==="
echo "Tailscale URL: http://${TS_IP}:8765"
echo "Token (simpan privat; jangan kirim ke siapa pun):"
grep '^ZERO_MANAGER_TOKEN=' /etc/zero-vps-manager.env | cut -d= -f2-
echo
echo "Health check:"
curl --silent --show-error "http://${TS_IP}:8765/v1/healthz" || true
echo
echo "Existing /opt/zero was not overwritten. No SSH or firewall settings were changed."
