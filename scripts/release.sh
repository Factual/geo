#! /usr/bin/env bash
set -euo pipefail

mkdir -p ~/.ssh
ssh-keyscan github.com >> ~/.ssh/known_hosts
mv geo_deploy_key ~/.ssh/id_rsa

VERSION=$(lein project-version)

echo "Project version is $VERSION"

if curl -If "https://clojars.org/factual/geo/versions/$VERSION"; then
  echo "$VERSION already exists in clojars. Continuing."
else
  echo "$VERSION does not exist in clojars. Will push it."
  lein midge
  lein deploy clojars
fi

if git ls-remote --tags origin | grep "$VERSION"; then
  echo "$VERSION tag already exists in GitHub. Continuing."
else
  echo "$VERSION tag does not exist in GitHub. Will push it."
  git tag $VERSION
  git push origin $VERSION
fi
