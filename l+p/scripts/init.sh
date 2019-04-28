#!/usr/bin/env bash

cd $(dirname "$0")/..

curl -s localhost:11515/runscript?resourceUrl=file:$(pwd|sed -e "s/+/%2B/")/src/main/resources/sql/create.sql
for i in {1..1000} ; do
    val=$(curl -s -X POST -H "Content-Type: text/plain" \
        -d "insert into message(context_id_, text_) values('$(uuid)', 'Message $(uuid)')" \
        localhost:11515/update)
    printf "\r%04d -> %-5d" "$i" "$val"
done

printf "\nDone"

