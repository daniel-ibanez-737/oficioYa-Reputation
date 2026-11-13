#!/usr/bin/env bash
# Levanta Reputation en Docker y ejecuta su colección de Postman con Newman.
# Requisitos: Docker y haber compilado el proyecto (./mvnw verify o ./mvnw package).
# Uso, desde la raíz del proyecto (Git Bash, Linux o macOS):  bash postman/ejecutar-coleccion.sh
RAIZ="$(cd "$(dirname "$0")/.." && (pwd -W 2>/dev/null || pwd))"
PROYECTO=oficioya-reputation
SERVICIO=oy-reputation
RED=oy-reputation-red
export MSYS_NO_PATHCONV=1

limpiar() {
  docker rm -f "$SERVICIO" oy-reputation-pg >/dev/null 2>&1
  docker network rm "$RED" >/dev/null 2>&1
}
limpiar
docker network create "$RED" >/dev/null

docker run -d --rm --name oy-reputation-pg --network "$RED" -e POSTGRES_DB=oficioya_reputation \
  -e POSTGRES_USER=oficioya -e POSTGRES_PASSWORD=oficioya postgres:16 >/dev/null
sleep 6

docker run -d --rm --name "$SERVICIO" --network "$RED" -e DB_URL=jdbc:postgresql://oy-reputation-pg:5432/oficioya_reputation -v "$RAIZ/target":/t eclipse-temurin:21-jre \
  sh -c "java -jar /t/$PROYECTO-*.jar --spring.profiles.active=demo" >/dev/null

for i in $(seq 1 60); do
  docker logs "$SERVICIO" 2>&1 | grep -q "Started Oficioya" && break
  sleep 2
done
echo "$SERVICIO: $(docker logs "$SERVICIO" 2>&1 | grep -o 'Started Oficioya[A-Za-z]* in [0-9.]* seconds' || echo 'NO ARRANCO')"

docker run --rm --network "$RED" -v "$RAIZ/postman":/etc/newman postman/newman:6.1.3-alpine run \
  OficioYa-Reputation.postman_collection.json \
  --env-var reputationUrl=http://$SERVICIO:8082 \
  --reporters cli --color off
RESULTADO=$?

echo "--- logs relevantes ---"
docker logs "$SERVICIO" 2>&1 | grep -E "Calificación registrada|Moderación registrada|Penalización aplicada|ERROR" | cut -c1-220 | head -6
limpiar
echo "newman exit=$RESULTADO"
exit $RESULTADO
