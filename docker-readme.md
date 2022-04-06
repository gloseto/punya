# Use Punya with docker

## Build a docker image containing the Punya source code

```
$ docker build --rm -t punya:latest .
```

## Create a container running Punya

```
$ docker container run -p 8888:8888 -it --name punya-dev --rm punya:latest
```

## Accessing your local server

```
http://localhost:8888
```

## Connect to an existing container running Punya (optional)

```
$ docker exec -it punya-dev /bin/bash
```

## Create and connect to a container without running Punya (optional)

```
$ docker container run -p 8888:8888 -it --name punya-dev --rm punya:latest /bin/bash
```