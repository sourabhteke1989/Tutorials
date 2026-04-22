FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="YourOrg <devops@yourorg.com>"
LABEL version="1.0.0"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/api-gateway-*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
