#!/usr/bin/env sh
set -eu

host="${1:-mqe-ueaz.tuyaus.com}"
port="${2:-7285}"
bundle="${TUYA_PULSAR_TLS_TRUST_CERTS_PATH:-certs/tuya-pulsar-ca-bundle.pem}"

echo "Endpoint: ${host}:${port}"
echo "Validación con autoridades del sistema:"
openssl s_client \
  -connect "${host}:${port}" \
  -servername "${host}" \
  -verify_return_error </dev/null 2>&1 \
  | grep -E 'subject=|issuer=|Verify return code|verify error' || true

echo "Validación con bundle temporal: ${bundle}"
openssl s_client \
  -connect "${host}:${port}" \
  -servername "${host}" \
  -CAfile "${bundle}" \
  -verify_return_error </dev/null 2>&1 \
  | grep -E 'subject=|issuer=|Verify return code|verify error' || true
