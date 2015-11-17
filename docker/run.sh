#!/bin/sh
readonly MACHINE_NAME=default

installed() {
  hash "$1" 2>/dev/null
}

if ! installed docker-machine; then
  brew cask install dockertoolbox
fi

if [[ -z $(docker-machine ls | grep $MACHINE_NAME) ]]; then
  docker-machine create -d virtualbox $MACHINE_NAME
fi

dockerenv=$(docker-machine env $MACHINE_NAME)
eval $dockerenv

docker run -ti -v "$HOME/.ivy2:/root/.ivy2"  -v "$PWD/..:/frontend" -p 9000:9000 desbo/frontend ./sbt
