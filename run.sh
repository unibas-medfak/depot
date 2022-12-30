#!/bin/sh

openssl req -x509 -nodes -newkey rsa:2048 -keyout key.pem -out cert.pem -sha256 -days 365 \
    -subj "/C=CH/ST=Basel/L=Basel/O=Unibas/OU=Medizin/CN=localhost"

docker build . -t depot

docker-compose up

