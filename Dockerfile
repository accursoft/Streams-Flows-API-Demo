FROM openjdk:jre-alpine

RUN adduser sda -S
USER sda
EXPOSE 8080
COPY . /home/sda

ENTRYPOINT ["/home/sda/bin/api-example"]