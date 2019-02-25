#! /usr/bin/env bash

lein release


# mkdir ~/.ssh
# ssh-keyscan github.com >> ~/.ssh/known_hosts
# echo $GITHUB_DEPLOY_KEY > ~/.ssh/id_rsa


# VERSION=$(lein project-version)

# echo "Project version is $VERSION"

# if curl -If "https://clojars.org/factual/geo/versions/$VERSION"; then
#   echo "$VERSION already exists in clojars. Continuing."
# else
#   echo "$VERSION does not exist in clojars. Will push it."
#   lein deploy clojars
# fi


# if git ls-remote --tags origin | grep "$VERSION"; then
#   echo "$VERSION tag already exists in GitHub. Continuing."
# else
#   echo "$VERSION tag does not exist in GitHub. Will push it."
# fi
