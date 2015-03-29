#!/bin/bash

set -e

cd `dirname $0`

if [ ! -d hosted ]; then
  git clone git@github.com:shaunlebron/solar-system-of-js.git hosted
fi

cd hosted

# checkout gh-pages branch
git checkout gh-pages

# make sure gh-pages is up-to-date
git pull

# remove all files
git rm -rf .

# add new report files
lein cljsbuild once release
cp -r ../resources/public/* .

# clean out unneeded
rm -rf js/out-adv \
       js/out \
       js/solar_system_of_js.js

# choose production page
mv index_release.html index.html

git add .
git commit -m "auto-update"

# publish to website
git push origin gh-pages
