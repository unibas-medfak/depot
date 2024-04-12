# depot
Simple and secure file storage service

## run local

```console
$ ./mvnw package
$ ./target/depot.jar
```
http://localhost:8080/swagger-ui/index.html

## run with docker compose

Edit run.sh

```console
$ ./build.sh
$ ./run.sh
```
https://localhost/swagger-ui/index.html

## run with docker

```console
$ docker build -t depot .
$ docker run -it -v ${PWD}/depot:/var/local/depot -p 8080:8080 depot

# -d
# -e DEFAULT_TENANT_PASSWORD='$2a...'
```

https://localhost/swagger-ui/index.html

## passwords

Passwords are stored as bcrypt hash https://bcrypt-generator.com/

## release

```console
mvn release:prepare
mvn release:perform -Darguments="-Dmaven.deploy.skip=true"
```

