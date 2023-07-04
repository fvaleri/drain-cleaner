# Developing Drain Cleaner

## Build

It can be build directly using Maven.
But it also has a simple Make build to make it easier to build the binary and the container image.

### Building the Java code with Maven

You can build the project using Maven.
With Java and Maven installed, you can run Maven to build the project.
```sh
mvn package
```

### Building the Java code using Make

You can also build the Java project with Make.
Make will still call Maven and Java, so you need to have them installed when using Make as well.
```sh
make java_package
```

You can pass additional arguments to the Maven build by setting the `MVN_ARGS` environment variable: 
```sh
MVN_ARGS=-B make java_package
```

### Building a container image manually

After you have the native executable, you can build the container manually:
```sh
docker build -f Dockerfile -t my-registry.tld/my-org/strimzi-drain-cleaner:latest .
docker push my-registry.tld/my-org/strimzi-drain-cleaner:latest
```

_Update the container image name to match your own registry / organization etc._

### Building a container image using Make

You can also build the image and push it into the registry using Make:
```sh
make docker_build docker_push
```

You can use the following environment variables to configure where will the image be pushed:
* `DOCKER_REGISTRY` defines the registry where it will be pushed. 
  For example `docker.io`.
* `DOCKER_ORG` defines the organization where it will be pushed. 
  For example `my-org`.
* `DOCKER_TAG` defines the tag under which will the image be pushed.
