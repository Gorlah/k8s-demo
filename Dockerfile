FROM adoptopenjdk/openjdk11:alpine-jre
WORKDIR /opt/app
COPY ./build/libs/k8s-demo-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
