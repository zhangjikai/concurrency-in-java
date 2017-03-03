#!/bin/sh
git add .
git commit -m "update"
cd /e/source/git/github/concurrency-in-java
git checkout master
git pull
git checkout gh-pages
git pull
cd /e/source/git/gitbook/zhangjk/concurrency-in-java
gitbook build
yes | cp -rf /e/source/git/gitbook/zhangjk/concurrency-in-java/_book/* /e/source/git/github/concurrency-in-java/
cd /e/source/git/github/concurrency-in-java
git checkout gh-pages
git add -A .
git commit -m "update"
git push
git checkout master
rsync -av --exclude='_book' --exclude='.git' --exclude='node_modules' --exclude='README.md' ../../gitbook/zhangjk/concurrency-in-java/ .
git add -A .
git commit -m "update"
git push