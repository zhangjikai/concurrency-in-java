#!/bin/sh
git add .
git commit -m "update"
cd /home/zhangjikai/文档/source_new/git/github/concurrency-in-java
git checkout master
git add -A .
git commit -m "update"
git pull
git stash save
git checkout gh-pages
git pull
cd /home/zhangjikai/GitBook/Library/zhangjk/concurrency-in-java
gitbook build
yes | cp -rf /home/zhangjikai/GitBook/Library/zhangjk/concurrency-in-java/_book/* /home/zhangjikai/文档/source_new/git/github/concurrency-in-java/
cd /home/zhangjikai/文档/source_new/git/github/concurrency-in-java
git add -A .
git commit -m "update"
git push
git checkout master
git stash pop
rsync -av --exclude='_book' --exclude='.git' --exclude='node_modules' --exclude='README.md' /home/zhangjikai/GitBook/Library/zhangjk/concurrency-in-java/ .
git add -A .
git commit -m "update"
git push