# Docker-Spray example
# VERSION 1.0

FROM gothug/xvfb-firefox

MAINTAINER Vasily Kozhukhovskiy

# copy the locally built fat-jar to the image
# ADD target/scala-2.11/docker-spray-example-assembly-1.0.jar /app/server.jar
ADD target/scala-2.11/server.jar /app/server.jar

# the server binds to 8080 - expose that port
EXPOSE 8080
ADD javaserver.sv.conf /etc/supervisor/conf.d/
