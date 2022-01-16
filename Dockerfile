# Secrets in this image require buildkit
# always run this build with
# DOCKER_BUILDKIT=1 docker build .

# https://hub.docker.com/_/maven
FROM maven:3-openjdk-17 as builder
LABEL maintainer = "Marco Luglio <marcodejulho@gmail.com>"

COPY . /usr/src/orderchange/

# default image workdir is /usr/src/myapp
WORKDIR /usr/src/orderchange/

# get secrets passed from docker build --secret id=mavenSecrets,src=mavenSecrets.txt .
# we can check the contents with cat /run/secrets/mavenSecrets
RUN --mount=type=secret,id=mavenSecrets

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

COPY --from=builder /usr/src/orderchange/target/release/container_azure_rust /bin/main
# probably not the correct place to put these
# COPY --from=builder /usr/src/main/hello.json /bin/hello.json

# user execute /bin/main
# user read /bin/hello.json and /bin/404.html
RUN chmod u+x /bin/main \
	&& chmod u+r /bin/hello.json \
	&& chmod u+r /bin/404.html

WORKDIR /bin
EXPOSE 80
ENTRYPOINT [ "/bin/main" ]
CMD ["java", "-jar", "a.jar"]