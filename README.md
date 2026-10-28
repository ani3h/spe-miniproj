# Calculator DevOps

A small Calculator REST API in plain Java, with a Jenkins → Docker → Docker Compose CI/CD pipeline.

The server uses the JDK's built-in `com.sun.net.httpserver` instead of a framework, so the app has
**no runtime dependencies**. The only dependency is JUnit 5, for tests.

## API

| Operation | Endpoint                       | Example response |
|-----------|--------------------------------|------------------|
| Add       | `GET /api/v1/add?a=2&b=3`      | `{"operation":"add","a":2,"b":3,"result":5}` |
| Subtract  | `GET /api/v1/subtract?a=5&b=3` | `{"operation":"subtract","a":5,"b":3,"result":2}` |
| Multiply  | `GET /api/v1/multiply?a=2&b=3` | `{"operation":"multiply","a":2,"b":3,"result":6}` |
| Divide    | `GET /api/v1/divide?a=5&b=2`   | `{"operation":"divide","a":5,"b":2,"result":2.5}` |
| Health    | `GET /actuator/health`         | `{"status":"UP"}` |

Errors return a JSON body with an HTTP status code:

- `400` for a missing, non-numeric or non-finite parameter, and for division by zero:
  `{"status":400,"error":"Division by zero is not allowed"}`
- `404` for an unknown operation
- `405` for any method other than GET

## Project layout

```
src/main/java/com/spe/calculator/
  Calculator.java        # pure arithmetic logic
  CalculatorServer.java  # HTTP routing, validation, JSON
  Main.java              # entry point (reads PORT, default 8080)
src/test/java/com/spe/calculator/
  CalculatorTest.java        # JUnit 5 unit tests
  CalculatorServerTest.java  # integration tests over real HTTP
Dockerfile           # multi-stage: Maven/JDK build -> JRE-alpine runtime, non-root
docker-compose.yml   # 2 calculator replicas + Nginx reverse proxy
nginx/nginx.conf
Jenkinsfile          # Checkout -> Build -> Test -> Docker Build -> Push -> Deploy
```

## Build, test, run (local)

You need JDK 17+ and Maven 3.9+.

```bash
mvn clean package          # compile, run tests, create target/calculator.jar
mvn test                   # tests only; JUnit XML goes to target/surefire-reports/
java -jar target/calculator.jar
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

## Docker

```bash
docker build -t calculator .
docker run -d -p 8080:8080 --name calculator calculator
curl http://localhost:8080/actuator/health
```

## Docker Compose (full stack)

```bash
docker compose up -d --build
docker compose ps          # 2 calculator replicas + nginx, all "healthy"
curl "http://localhost:8081/api/v1/divide?a=10&b=4"
docker compose down
```

Nginx listens on host port `8081`, since Jenkins uses 8080 (override with `HOST_PORT`), and round-robins across the replicas.

## Jenkins setup

1. Install the plugins **Pipeline**, **Git**, **GitHub**, **JUnit**, **Credentials Binding** and **Timestamper**.
   Make sure `mvn`, `docker` and `curl` are on the Jenkins agent's PATH.
2. Add the Docker Hub credentials: **Manage Jenkins → Credentials → Username with password**.
   Set the ID to `dockerhub-creds` and use a Docker Hub access token as the password.
3. Set up email: **Manage Jenkins → System → E-mail Notification**. For Gmail, use SMTP `smtp.gmail.com`,
   port 465 with SSL, and an app password.
4. In the `Jenkinsfile`, change `DOCKER_IMAGE` and `NOTIFY_EMAIL` to your own values.
5. Create a **Pipeline** job with *Pipeline script from SCM*, pointing at this repo and `Jenkinsfile`.
6. In the GitHub repo, go to **Settings → Webhooks** and add `http://<jenkins-host>/github-webhook/`
   (content type `application/json`, push events). If Jenkins runs on localhost, expose it with
   `ngrok http 8080`, or use the `pollSCM` trigger the Jenkinsfile already includes.

On each push, the pipeline:

| Stage        | What it does |
|--------------|--------------|
| Checkout     | `checkout scm` |
| Build        | `mvn clean package -DskipTests` |
| Test         | `mvn test`, publishes JUnit results, fails the build on a test failure |
| Docker Build | tags the image `<repo>:<BUILD_NUMBER>` and `<repo>:latest` |
| Push         | logs in to Docker Hub with Jenkins credentials and pushes both tags |
| Deploy       | `docker compose up -d` with the new tag, then polls `/actuator/health` |

The pipeline then sends an email for both success and failure. You can safely re-run the Deploy stage:
Compose only recreates containers whose image changed.
