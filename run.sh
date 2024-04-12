#!/bin/sh

mkdir -p ssl

openssl req -x509 -nodes -newkey rsa:2048 -keyout ssl/key.pem -out ssl/cert.pem -sha256 -days 365 \
    -subj "/C=CH/ST=Basel/L=Basel/O=Unibas/OU=Medizin/CN=localhost"

docker build . -t depot

# https://bcrypt-generator.com
export DEFAULT_TENANT_PASSWORD='$2a$ ... iAbyBG'

docker-compose up
