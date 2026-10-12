# ZERO VPS Manager — redesigned UI prototype

Android native Kotlin + Jetpack Compose concept for a unified Home / VPS / Desktop / Settings app, inspired by modern Windows remote desktop workflows and Termius-style connection management.

## Included in this package
- Redesigned dark UI with Home, VPS, Desktop, and Settings navigation.
- Add connection profile form: name, host/IP, port, username, connection type.
- VPS tools menu concept: File Manager, Code Editor, Projects, Docker/Services, Logs/Monitoring, Terminal.
- Desktop remote-PC placeholder and controls roadmap.
- Settings fields for the original Tailscale agent URL and bearer token.
- Existing FastAPI agent and GitHub Actions APK workflow retained.

## Important implementation status
This is a **UI prototype**, not a finished remote-management client. Saved connection profiles are in-memory only. SSH authentication/file transfer/editor/project execution and RDP screen streaming are NOT implemented in this revision. The password field is intentionally not saved. Agent settings are stored in ordinary Android preferences in this prototype; do not treat that as production-grade secret storage. The existing agent features have not been build-verified in this environment.

## Build APK
Upload the contents of this folder to a GitHub repository and run the workflow at `.github/workflows/build-apk.yml`. Review the build logs before installing. Do not install on a production VPS expecting full SSH/RDP support yet.

## Safety
The installer for the optional agent uses a separate `/opt/zero-vps-manager` directory and is intended not to alter `/opt/zero`, database, SSH configuration, or existing services. Review scripts before executing them on a production VPS.
