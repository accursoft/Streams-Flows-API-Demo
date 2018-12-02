#!/usr/bin/env sh

docker run --name streams-flows-api-test -d --rm accursoft/streams-flows-api:latest
sleep 3
result=$(docker exec streams-flows-api-test wget -qO- localhost:8080)
docker stop streams-flows-api-test
[ "${result#*Streams Flows API Demo}" != "$result" ]