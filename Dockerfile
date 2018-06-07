FROM openjdk:jre-alpine
EXPOSE 8080
COPY . /root
ENTRYPOINT ["/root/bin/api-example"]