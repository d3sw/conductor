FROM amazoncorretto:8 as builder
WORKDIR /stage
ADD . /stage
RUN ./gradlew build -x test

#
# conductor:server - Netflix conductor server
#
FROM amazoncorretto:8

MAINTAINER Netflix OSS <conductor@netflix.com>

# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs

# Copy the project directly onto the image
COPY --from=builder /stage/docker/server/bin /app
COPY --from=builder /stage/docker/server/config /app/config
COPY --from=builder /stage/docker/server/lib/dd-java-agent-0.88.0.jar /app/libs
COPY --from=builder /stage/server/build/libs/conductor-server-*-all.jar /app/libs

# Copy the files for the server into the app folders
RUN chmod +x /app/startup.sh

EXPOSE 8080
CMD [ "/app/startup.sh" ]