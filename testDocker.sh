#!/usr/bin/env sh

docker run --name streams-designer-api-test -d --rm accursoft/streams-designer-api:latest
sleep 2
result=$(docker exec streams-designer-api-test wget -qO- localhost:8080)
docker stop streams-designer-api-test
[ "${result#*Streams Designer API Demo}" != "$result" ]