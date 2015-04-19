Deploy HowTo
============
1. Start docker daemon on Mac OS

    launch boot2docker app (std password - tcuser)

2. Prepare environment to use docker

    ```
    export DOCKER_HOST=tcp://192.168.59.103:2376
    export DOCKER_CERT_PATH=/Users/kojuhovskiy/.boot2docker/certs/boot2docker-vm
    export DOCKER_TLS_VERIFY=1
    ```

3. Build jar file & try server locally:

   ```
   sbt assembly
   java -jar docker-spray-[...].jar
   ```

3. Build new docker image for user gothug called spray-docker

    ```
    docker build -t="gothug/spray-docker" .
    ```

4. Run image locally (optional) (correctly substitute image id, last param in docker run)

    ```
    docker run -d -p 9090:8080 gothug/spray-docker
    boot2docker ip
    curl "http://192.168.59.103:9090/hello"
    ```

5. Push docker image to docker hub

    ```
    docker push gothug/spray-docker
    ```

6. Pull & run image on Digital Ocean instance (correctly substitute image id, last param in docker run)

    ```
    ssh -i ~/.ssh/id_rsa_digitalocean root@188.166.11.149
    docker pull gothug/spray-docker
    docker run -d -p 9090:8080 82a7df479a58
    ```

7. Send a test request to the deployed server

    ```
    curl "http://188.166.11.149:9090/hello"
    ```

Deploy locally from digital ocean server (the fastest way)
==========================================================
0. Get to server

    ssh -i ~/.ssh/id_rsa_digitalocean root@188.166.11.149

1. Build && start postgresql container

    git clone https://github.com/gothug/docker-configs
    cd docker-configs/postgresql
    git pull; docker build -t="gothug/postgresql" .
    docker run -d -p 5432:5432 --name postgres gothug/postgresql

    cd ~/movie-geek
    JAVA_OPTS="-Xms250m -Xmx384m" sbt "run-main mvgk.db.DBManager drop create"

2. Build && start movie service

    git clone https://github.com/gothug/docker-spray-example.git
    cd docker-spray-example
    JAVA_OPTS="-Xms250m -Xmx384m" sbt assembly
    docker build -t="gothug/spray-docker" .
    docker run -d -p 9090:8080 --link postgres:pgsql gothug/spray-docker

Test requests
=============
1. General

    curl "http://localhost:8080/request"
    curl -v -X POST http://localhost:8080/request -H "Content-Type: application/json" -d '{"name": "Bob", "firstName": "Parr", "age": 32}'

    curl -X POST http://localhost:8080/watchlist/imdb -H "Content-Type: application/json" -d '{"link": "http://www.quickproxy.co.uk/index.php?q=aHR0cDovL3d3dy5pbWRiLmNvbS91c2VyL3VyOTExMjg3OC93YXRjaGxpc3Q%2FcmVmXz13dF9udl93bF9hbGxfMA%3D%3D&hl=2ed"}'

2. User service

    curl http://localhost:8080/user
    curl -X POST http://localhost:8080/user -H "Content-Type: application/json" -d '{"email": "kojuhovskiy@gmail.com", "subscribed": false}'

Useful tips
===========
1. Extract a particular file from jar

    jar xvf ~/github/docker-spray-example/target/scala-2.11/docker-spray-example-assembly-1.0.jar reference.conf

2. See memory usage per process:

    ps -C -O rss

3. Run java on a box with 512Mb of memory:

    JAVA_OPTS="-Xms250m -Xmx384m" sbt assembly

Useful docker tips
==================
1. Run bash inside a docker container:

    sudo docker exec -it <containerIdOrName> bash

    sudo docker exec -it `docker ps -a | tail -n1 | cut -d' ' -f1` bash

2. Get into an image:

    docker run -t -i gothug/postgresql /bin/bash

3. Delete all docker images:

    docker images | awk '{print $3}'  | xargs docker rmi

Database
========
0. Setup

    CREATE USER geek WITH SUPERUSER PASSWORD 'q1';
    CREATE DATABASE geek WITH OWNER = geek;

1. Db access

    pg_ctl -D /usr/local/var/postgres -l /usr/local/var/postgres/server.log start
    psql -d moviedb -U geek

2. Db create

    sbt "run-main mvgk.db.DBManager create"

3. Db drop create

    sbt "run-main mvgk.db.DBManager drop create"

4. Db update

    sbt "run-main mvgk.db.DBManager update"
