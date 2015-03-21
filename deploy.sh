#!/bin/sh

echo 'Deploy script'

## Local commands
echo
echo "- local git push:"
git push

## Remote commands
cdCmd='cd /root/docker-spray-example/;'

gitPullCmd='echo; echo "- remote git pull:"; git pull;'

sbtAssemblyCmd='echo; echo "- sbt assembly:"; JAVA_OPTS="-Xms50m -Xmx300m" sbt assembly;'

createSoftLinkForLatestJarCmd='echo; echo "- create softlink to jar:"; cd target/scala-*.*; ln -sf `find . -maxdepth 1 -name "*.jar" ! -type l | xargs ls -t | head -n1` server.jar; cd -;'

dockerBuildCmd='echo; echo "- docker build:"; docker build -t="gothug/spray-docker" .;'

dockerStopContainerCmd='echo; echo "- docker stop container:"; docker stop moviegeek;'

dockerRemoveContainerCmd='echo; echo "- docker remove container:"; docker rm moviegeek;'

updateDbCmd='echo; echo "- update db:"; JAVA_OPTS="-Xms250m -Xmx384m" sbt "run-main mvgk.db.DBManager update";'

dockerStartContainerCmd='echo; echo "- docker start container:"; docker run -d -p 9090:8080 --link postgres:pgsql --name moviegeek gothug/spray-docker;'

#ssh -i ~/.ssh/id_rsa_digitalocean root@104.131.98.252 "$cdCmd $gitPullCmd $sbtAssemblyCmd $createSoftLinkForLatestJarCmd $dockerBuildCmd $dockerStopContainerCmd $dockerRemoveContainerCmd $updateDbCmd $dockerStartContainerCmd"
ssh -i ~/.ssh/id_rsa_vdsina root@109.234.35.251 "$cdCmd $gitPullCmd $sbtAssemblyCmd $createSoftLinkForLatestJarCmd $dockerBuildCmd $dockerStopContainerCmd $dockerRemoveContainerCmd $updateDbCmd $dockerStartContainerCmd"
