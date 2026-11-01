#!/usr/bin/env bash
set -euo pipefail
umask 077
[[ $EUID -eq 0 ]] || { echo 'Run as root on a dedicated Ubuntu 24.04 VPS'; exit 1; }
source /etc/os-release
[[ $ID == ubuntu && $VERSION_ID == 24.04 ]] || { echo 'Ubuntu 24.04 required'; exit 1; }
[[ -c /dev/net/tun ]] || { echo 'Provider must enable TUN'; exit 1; }
[[ -f /etc/usa-favor/gateway.json ]] || { echo 'Create /etc/usa-favor/gateway.json from template first (mode 600)'; exit 1; }
cd -- "$(dirname -- "$0")"
python3 - <<'PY'
import json
from agent import validate
validate(json.load(open('/etc/usa-favor/gateway.json')))
PY
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y openvpn easy-rsa nginx certbot python3-certbot-nginx nftables
install -d -m 700 /etc/usa-favor /etc/openvpn/server /var/lib/usa-favor
install -m 600 /etc/usa-favor/gateway.json /var/lib/usa-favor/deployed.json
install -d -m 755 /usr/local/lib/usa-favor
install -m 755 agent.py auth-hook.py /usr/local/lib/usa-favor/
if [[ ! -f /etc/openvpn/server/server.key ]]; then
 [[ -d /var/lib/usa-favor/pki-work ]] || make-cadir /var/lib/usa-favor/pki-work
 cd /var/lib/usa-favor/pki-work
 export EASYRSA_BATCH=1 EASYRSA_ALGO=ec EASYRSA_CURVE=prime256v1
 [[ -d pki ]] || ./easyrsa init-pki
 if [[ ! -f pki/ca.crt ]]; then ./easyrsa build-ca nopass; fi
 [[ -f pki/private/ca.key ]] || { echo "Issuing CA key is missing; restore it securely before provisioning"; exit 1; }
 HOST=$(python3 -c "import json;print(json.load(open('/etc/usa-favor/gateway.json'))['host'])")
 [[ -f pki/issued/$HOST.crt ]] || ./easyrsa build-server-full "$HOST" nopass
 ./easyrsa gen-crl
 install -m 644 pki/ca.crt /etc/openvpn/server/ca.crt
 install -m 644 "pki/issued/$HOST.crt" /etc/openvpn/server/server.crt
 install -m 600 "pki/private/$HOST.key" /etc/openvpn/server/server.key
 install -m 644 pki/crl.pem /etc/openvpn/server/crl.pem
 openvpn --genkey tls-crypt-v2-server /etc/openvpn/server/tls-crypt-v2.key
 # The issuing CA is root-owned and needed for per-session issuance. Never export it to Firebase or APKs.
fi
python3 - <<'PY'
import json
from pathlib import Path
c=json.load(open('/etc/usa-favor/gateway.json'))
conf=f'''port {c['port']}
proto {'udp' if c['transport']=='udp' else 'tcp-server'}
dev tun-usa
server 10.87.0.0 255.255.255.0
server-ipv6 fd42:87::/64
topology subnet
ca /etc/openvpn/server/ca.crt
cert /etc/openvpn/server/server.crt
key /etc/openvpn/server/server.key
dh none
tls-version-min 1.2
data-ciphers AES-256-GCM:AES-128-GCM
auth SHA256
tls-crypt-v2 /etc/openvpn/server/tls-crypt-v2.key
verify-client-cert require
username-as-common-name
auth-user-pass-verify /usr/local/lib/usa-favor/auth-hook.py via-file
script-security 2
reneg-sec 0
crl-verify /etc/openvpn/server/crl.pem
push "redirect-gateway def1 ipv6"
push "block-ipv6"
keepalive 10 60
persist-key
persist-tun
max-clients {c['max_sessions']}
management /run/openvpn-server/usa-favor.sock unix
status /run/openvpn-server/usa-status 10
verb 2
'''
for ip in c['dns']:conf+=f'push "dhcp-option DNS {ip}"\n'
Path('/etc/openvpn/server/usa-favor.conf').write_text(conf)
PY
printf 'net.ipv4.ip_forward=1\n' > /etc/sysctl.d/70-usa-favor.conf
sysctl --system
WAN=$(ip -4 route show default | awk 'NR==1 {print $5}')
[[ $WAN =~ ^[a-zA-Z0-9_.:-]+$ ]]
# Add dedicated rules; never flush existing firewall rules. No inbound SSH rules are changed.
if ! nft list table ip usa_favor >/dev/null 2>&1; then
 nft -f - <<RULES
table ip usa_favor {
 chain nat { type nat hook postrouting priority srcnat; ip saddr 10.87.0.0/24 oifname "$WAN" masquerade; }
 chain forward { type filter hook forward priority -10; policy accept; iifname "tun-usa" ip daddr { 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, 169.254.0.0/16, 127.0.0.0/8 } drop; iifname "tun-usa" oifname != "$WAN" drop; }
}
table inet usa_favor_guard {
 chain input { type filter hook input priority -10; policy accept; iifname "tun-usa" drop; }
 chain forward { type filter hook forward priority -10; policy accept; iifname "tun-usa" meta nfproto ipv6 drop; }
}
RULES
fi
nft list table ip usa_favor > /etc/usa-favor/firewall.nft
nft list table inet usa_favor_guard >> /etc/usa-favor/firewall.nft
cat > /etc/systemd/system/usa-favor-firewall.service <<'UNIT'
[Unit]
Description=USA Favor VPN routing firewall
Before=openvpn-server@usa-favor.service
After=network-pre.target
[Service]
Type=oneshot
ExecStart=/bin/sh -c '/usr/sbin/nft list table ip usa_favor >/dev/null 2>&1 && /usr/sbin/nft list table inet usa_favor_guard >/dev/null 2>&1 || /usr/sbin/nft -f /etc/usa-favor/firewall.nft'
RemainAfterExit=yes
[Install]
WantedBy=multi-user.target
UNIT
cat > /etc/systemd/system/usa-favor-agent.service <<'UNIT'
[Unit]
Description=USA Favor gateway management and authorization
After=network-online.target
Wants=network-online.target
[Service]
ExecStart=/usr/bin/python3 /usr/local/lib/usa-favor/agent.py
Restart=on-failure
RuntimeDirectory=usa-favor
RuntimeDirectoryMode=0755
UMask=0077
NoNewPrivileges=yes
ProtectHome=yes
ProtectSystem=strict
ReadWritePaths=/etc/openvpn/server /etc/usa-favor /var/lib/usa-favor /run/usa-favor /run/openvpn-server
PrivateTmp=yes
[Install]
WantedBy=multi-user.target
UNIT
install -d /etc/systemd/system/openvpn-server@usa-favor.service.d
cat > /etc/systemd/system/openvpn-server@usa-favor.service.d/authorization.conf <<'UNIT'
[Unit]
Requires=usa-favor-agent.service usa-favor-firewall.service
After=usa-favor-agent.service usa-favor-firewall.service
BindsTo=usa-favor-agent.service
UNIT
systemctl daemon-reload
systemctl enable usa-favor-firewall usa-favor-agent openvpn-server@usa-favor
systemctl restart usa-favor-agent openvpn-server@usa-favor
HOST=$(python3 -c "import json;print(json.load(open('/etc/usa-favor/gateway.json'))['host'])")
cat > /etc/nginx/sites-available/usa-favor <<NGINX
server {
 listen 80;
 server_name $HOST;
 client_max_body_size 8k;
 location /control/ { proxy_pass http://127.0.0.1:9080; proxy_read_timeout 15s; }
 location / { return 404; }
}
NGINX
ln -sf /etc/nginx/sites-available/usa-favor /etc/nginx/sites-enabled/usa-favor
nginx -t
systemctl reload nginx
printf '\nFinish HTTPS: certbot --nginx -d %s --redirect\nAllow TCP 443 and configured VPN port in provider firewall. Management is never usable without its random key.\n' "$HOST"
