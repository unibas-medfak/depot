# depot
Simple and secure file storage service

## run local

```console
$ ./mvnw package
$ ./target/depot.jar
```
http://localhost:8080/swagger-ui/index.html

## run with docker

```console
$ ./build.sh
$ ./run.sh
```
https://localhost/swagger-ui/index.html

## release

```console
mvn release:prepare
mvn release:perform -Darguments="-Dmaven.deploy.skip=true"
```

