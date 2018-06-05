FROM openjdk:jre-alpine
EXPOSE 8080
COPY . /root
CMD /root/bin/api-example