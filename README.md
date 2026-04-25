# depot
Simple and secure file storage service

Depot is a multi-tenant file storage service built with Java 25 and Spring Boot 4.1 that provides secure, role-based file management through a RESTful API. The application features JWT authentication with realm-based access control, supporting READ, WRITE, and DELETE permissions for organized file operations. Built with Spring Security for authentication, BCrypt password hashing for secure credential storage, and comprehensive audit logging, Depot ensures secure multi-tenant isolation with tenant/realm directory structures. Optional per-tenant soft-delete and write-time backup keep prior versions recoverable. The service ships with a React frontend for browsing, OpenAPI/Swagger documentation, QR codes that contain a direct login URL into the frontend, and can be deployed as a standalone JAR, a GraalVM native image, or via Docker with optional Docker Compose orchestration.

## run local

```console
$ ./mvnw package
$ java -jar target/depot.jar
```
http://localhost:8080/  (frontend) — http://localhost:8080/swagger-ui/index.html  (API docs, set `springdoc.swagger-ui.enabled=true` to enable)

## build a native image

Requires GraalVM (CE or Oracle).

```console
$ ./mvnw -Pnative -DskipTests native:compile
$ ./target/depot
```

## run with docker compose

Edit docker-run.sh

```console
$ ./docker-build.sh
$ ./docker-run.sh
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
All file operations require JWT authentication with role-based access control. Missing or invalid credentials return `401`; authenticated requests that lack the required role return `403`.

The `expirationDate` field on admin requests accepts either an ISO date (`yyyy-MM-dd`, interpreted as UTC start of day) or a full ISO date-time (e.g. `2025-12-31T23:59:59Z`, `2025-12-31T23:30:00+02:00`).

### Admin Endpoints (`/admin`, public)
- `POST /admin/register` - Get JWT access token for a realm
- `POST /admin/qr` - PNG QR code containing a direct frontend login URL (`<host>/#token=<jwt>`)

### File Operations
- `GET /list?path=<path>` - List files/folders (requires READ role)
- `GET /get?file=<filepath>` - Download file (requires READ role)
- `POST /put` - Upload file with multipart/form-data (requires WRITE role)
- `GET /move?fromPath=<src>&toPath=<dst>` - Move/rename a file or folder (requires WRITE role; refuses to overwrite an existing destination)
- `GET /delete?path=<path>` - Delete file/folder (requires DELETE role; honors per-tenant soft-delete)

### Info
- `GET /info` - Service version + GitHub and Swagger URLs (public, JSON)
- `GET /` - React frontend (browser, preview, login terminal)

See Swagger UI for detailed API documentation and testing interface.

## passwords

Passwords are stored as bcrypt hash https://bcrypt-generator.com/

## release

Releases are tag-driven. To cut version `1.2.3`:

```console
$ ./mvnw versions:set -DnewVersion=1.2.3 -DgenerateBackupPoms=false
$ git commit -am "Release 1.2.3"
$ git tag v1.2.3
$ git push origin master v1.2.3
```

Pushing the `v1.2.3` tag triggers three workflows:
- `release.yml` — builds the Linux x86_64 native image and creates a GitHub Release with auto-generated notes and the binary attached.
- `docker-publish.yml` — builds and pushes the multi-arch Docker image to ghcr.io.
- `bump-snapshot.yml` — opens a PR bumping `pom.xml` to the next patch `-SNAPSHOT` (e.g. `1.2.4-SNAPSHOT`). Review and merge it.

The release workflow refuses to run if `pom.xml`'s version doesn't match the tag.

