# depot
Simple and secure file storage service

Depot is a multi-tenant file storage service built with Java 25 and Spring Boot 4.0.0 that provides secure, role-based file management through a RESTful API. The application features JWT authentication with realm-based access control, supporting READ, WRITE, and DELETE permissions for organized file operations. Built with Spring Security for authentication, BCrypt password hashing for secure credential storage, and comprehensive audit logging, Depot ensures secure multi-tenant isolation with tenant/realm directory structures. The service includes OpenAPI/Swagger documentation for easy API exploration, QR code generation for simplified realm access provisioning, and can be deployed as a standalone JAR or via Docker with optional Docker Compose orchestration.

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

## API Documentation

### Authentication
All file operations require JWT authentication with role-based access control.

### Admin Endpoints (`/admin`)
- `POST /admin/register` - Get access token for realm
- `POST /admin/qr` - Generate QR code for realm access  
- `POST /admin/log` - Get last 100 log events

### File Operations
- `GET /list?path=<path>` - List files/folders (requires READ role)
- `GET /get?file=<filepath>` - Download file (requires READ role)
- `POST /put` - Upload file with multipart/form-data (requires WRITE role)
- `GET /delete?path=<path>` - Delete file/folder (requires DELETE role)

### Info
- `GET /` - Application banner and version info

See Swagger UI for detailed API documentation and testing interface.

## passwords

Passwords are stored as bcrypt hash https://bcrypt-generator.com/

## release

```console
mvn release:prepare
mvn release:perform -Darguments="-Dmaven.deploy.skip=true"
```

