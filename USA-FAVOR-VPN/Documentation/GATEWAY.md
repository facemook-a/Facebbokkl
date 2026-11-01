# Deploy an operational US OpenVPN gateway

This package does not contain an existing US VPS, public IP, management key, CA private key, hosting account or working VPN entitlement. Provisioning requires a VPS physically hosted in the United States with Ubuntu 24.04, public IPv4, `/dev/net/tun`, root installation rights and outbound HTTPS. Confirm location with your provider; changing a display country does not change egress location.

## First deployment

1. Provision the VPS and a domain you control (A record pointing at its real public IPv4). Remove an unusable AAAA record for this IPv4-only management deployment. Confirm the real NAT egress address with your provider. Preserve provider/SSH access and existing firewall policies.
2. Create a disabled registration in Admin. Copy its gateway ID. Securely create a random per-gateway management key with at least 32 bytes of entropy. Store it as Secret Manager secret `usa-favor-gateway-ID` and give the deployed Functions runtime account read access. The gateway needs the same key; Android never receives it. See FIREBASE.md.
3. Copy `VPN-Gateway/` onto the VPS. Create `/etc/usa-favor/gateway.json` from `gateway.example.json`, owned by root, mode 600. Set the actual gateway ID, domain, public egress IPv4, port, transport, public IPv4 DNS and capacity (1–200). The example file cannot run unchanged. The authorization URL must be the deployed usa-favor gatewayAuthorize endpoint.
4. Run `sudo bash install.sh` on that dedicated Ubuntu 24.04 VPS. Review the script before running. It installs packages, creates an issuing EC CA and server certificate, per-session issuance tooling, OpenVPN, nftables NAT/guard rules, systemd services and nginx reverse proxy. It does not flush existing firewall tables or change SSH input policy. Existing provider/UFW rules must allow the intended tunnel routing.
5. Finish trusted HTTPS with `sudo certbot --nginx -d YOUR_ACTUAL_DOMAIN --redirect`. Certbot requires valid public DNS and its ACME challenge port. Test renewal. Restrict direct ingress to agent port 9080 (the agent binds loopback only). Open TCP 443 and the configured UDP/TCP VPN port in the provider firewall; TCP 80 is needed for the selected certificate renewal method. No management authorization is sent over HTTP.
6. Check `systemctl status usa-favor-agent openvpn-server@usa-favor usa-favor-firewall`, certificate files, `nginx -t`, and logs with credential redaction. Keep root-only encrypted backups of the issuer, EasyRSA database, server keys and configuration. The online issuer is required to issue per-session client certificates; isolate and harden this dedicated VPS. Never upload these private files to Firebase, APKs, Git or chat.
7. In Admin, align hostname/port/transport/DNS/capacity with the VPS. Enable, save, TEST and PUBLISH. A failed health or identity/configuration match blocks publication. Saving registration alone does not make a server operational.

## Management after deployment

- Name, location, flag, ordering/priority and primary/backup selection are delivered remotely after save/verification/publication.
- For a supported port/transport/DNS/capacity change: edit/save the registration, press **configure**, then test and publish. Configure sends only predefined validated fields to the authenticated gateway, restarts OpenVPN and rolls back its listener file on a failed restart. Allow new ports in the provider firewall first. Existing connections are interrupted during this change.
- An IP/hostname move requires actual DNS/HTTPS/OpenVPN certificates and gateway configuration to match the destination. Transfer/provision the registered gateway identity and its secret securely at the new owned host, then edit/test/publish. Never point an existing identity at an untrusted host: management credentials are sent only to the registered HTTPS endpoint. No Client APK changes are involved.
- Disable/maintenance/removal stops authorization. Removal unregisters the server; it does not delete a VPS or cancel hosting charges.
- **rotate** rotates the tls-crypt-v2 wrapping key and restarts the service, terminating existing sessions. Clients request new authorized profiles on reconnect. CA/server-certificate and management-key rotation require the documented infrastructure/Secret Manager changes; the button does not pretend to rotate the whole PKI.
- The root agent refreshes the CRL every 12 hours. Subscription revocation also kills the matching live session and revokes its client certificate. Retain issuer databases for revocation integrity. Keep system clocks synchronized.

## Required live acceptance (not executed here)

Use a physical Android device with matching Firebase/App Check setup and a real gateway:

1. Verify unauthenticated control requests fail. Verify direct Firestore access fails. The backend's authenticated health call must confirm the real OpenVPN daemon and matching gateway ID.
2. Activate a real test license in Client. Approve VPN permission and notification permission. Connect. Independently confirm Android's VPN indicator, OpenVPN management CLIENT_LIST, assigned tunnel addresses, DNS resolution and HTTPS reachability.
3. Verify the app reports Connected only after its VPN-bound HTTPS egress check equals the gateway's actual public address. Check IPv4 full routing, IPv6 leak blocking and DNS from outside the VPS. Verify LAN/private/metadata destinations are inaccessible through the gateway.
4. Check real tunnel bytes, connection timer, disconnect cleanup, Wi-Fi/mobile changes, foreground notification, background/process recreation and reconnect errors. Test Android always-on/lockdown settings on supported devices.
5. While connected, revoke the license, expire a short test license, reset binding, disable the gateway and stop backend authorization. Confirm denial of reconnect and termination during the enforcement sweep. Latency includes the 15-second sweep interval, network timeout and bounded validation batches; measure it at your intended load. Stopping/crashing the agent must also stop OpenVPN through systemd.
6. Test tls-crypt-v2 rotation, certificate revocation/CRL renewal, provider port changes, a second gateway, primary/backup changes and server-list synchronization without changing the Client APK.
7. Record actual device/OS versions, server IP/location, DNS/IPv6 results, timestamps and measured revocation latency. Only then claim a working VPN service.

This Ubuntu/systemd/nftables deployment has been syntax/unit checked, not installed on a VPS in this session. Validate the install, rollback, reboot behavior and routing in staging before selling access.
