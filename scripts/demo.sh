#!/usr/bin/env bash
# Demo script for the Delivery Fee Service.
# Assumes the app is running on http://localhost:8080 (./mvnw spring-boot:run).
set -euo pipefail

BASE="http://localhost:8080"
CITY="TARTU"
VEHICLE="BIKE"

echo "== Delivery Fee Service demo =="
echo

echo "1) Health check"
curl -s "${BASE}/actuator/health"; echo; echo

echo "2) Current fee for ${CITY} + ${VEHICLE}"
curl -s "${BASE}/api/v1/delivery-fee?city=${CITY}&vehicleType=${VEHICLE}"; echo; echo

echo "3) Historical fee (Jan 2024) for ${CITY} + ${VEHICLE}"
curl -s "${BASE}/api/v1/delivery-fee?city=${CITY}&vehicleType=${VEHICLE}&datetime=2024-01-15T12:00:00Z"; echo; echo

echo "4) List base fee rules"
curl -s "${BASE}/api/v1/rules/base-fees"; echo; echo

echo "5) Create a historical base fee rule (non-overlapping window)"
CREATED=$(curl -s -X POST "${BASE}/api/v1/rules/base-fees" \
  -H 'Content-Type: application/json' \
  -d '{"city":"TALLINN","vehicleType":"CAR","fee":5.00,"validFrom":"1990-01-01T00:00:00Z","validTo":"1995-01-01T00:00:00Z"}')
echo "$CREATED"; echo

ID=$(echo "$CREATED" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
if [ -n "$ID" ]; then
  echo "6) Delete the created rule (id=${ID})"
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE "${BASE}/api/v1/rules/base-fees/${ID}"
  echo
fi

echo "7) Invalid enum value (lowercase) — returns 400"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "${BASE}/api/v1/delivery-fee?city=tartu&vehicleType=${VEHICLE}"
echo

echo "8) Future datetime — returns 400"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "${BASE}/api/v1/delivery-fee?city=${CITY}&vehicleType=${VEHICLE}&datetime=2999-01-01T00:00:00Z"
echo

echo "Done. Swagger UI: ${BASE}/swagger-ui.html"
