#!/usr/bin/env bash

function usage() {
    echo "Usage: $0 <sql-string|@sql-file> <output-file>"
    exit 1
}

if [[ $# != 2 ]] ;then
    usage
fi

sql=$1
output=$2

wd=$(dirname "$0")

touch "${output}"
if [[ $(cat "${output}" | wc -l) -gt 0 ]] ; then
    head -1 "${output}"
fi

(while [[ true ]] ; do
    skip=$(( $(cat "${output}"|wc -l|tr -d " \t") + 1))
    ${wd}/sql.sh tsv "${sql}" | tail -n +${skip} >>"${output}"
    sleep 0.2
done) &
pid=$!

trap 'kill ${pid}' EXIT

tail -20f "${output}"
