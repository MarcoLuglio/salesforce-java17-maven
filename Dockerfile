# Secrets in this image require buildkit
# always run this build with
# DOCKER_BUILDKIT=1 docker build .

# https://hub.docker.com/_/maven
FROM maven:3-openjdk-17 as builder
LABEL maintainer = "Marco Luglio <marcodejulho@gmail.com>"

# pass a value for this argument with
# docker build --build-arg arg1=myvalue .
# ARG is discarded after build
ARG ARG1=value2

# overwrite this value with
# docker run ... -e MAVEN_CONFIG=/var/maven/.m2
# ENV is persisted after build
# ENV ARG1=${ARG1:-myValue}
ENV VAR1 value1
ENV VAR2 "$ARG1"

# get secrets passed from docker build --secret id=mavenSecrets,src=mavenSecrets.txt .
# we can check the contents with cat /run/secrets/mavenSecrets
RUN --mount=type=secret,id=mavenSecrets

COPY . /usr/src/orderchange/

# default image workdir is /usr/src/myapp
WORKDIR /usr/src/orderchange/

# add authentication form environment varibles
RUN sed -i 's/replacedByThePipelineUsername/${{ secrets.nexusUsername }}/g' settings.xml
RUN sed -i 's/replacedByThePipelinePasssword/${{ secrets.nexusPassword }}/g' settings.xml

# download, unzip and add maven bin to path

RUN mvn -B compile -s settings.xml
RUN mvn -B test -s settings.xml
RUN mvn -B package -s settings.xml

###############################################

# https://hub.docker.com/_/openjdk
FROM openjdk:17.0.1

COPY --from=builder /usr/src/orderchange/target/salesforce-java17-maven-1.0.jar /bin/main.jar

# user execute /bin/main
RUN chmod u+x /bin/main.jar

WORKDIR /bin
# EXPOSE 80
ENTRYPOINT ["java", "-jar"]
CMD ["/bin/main.jar"]