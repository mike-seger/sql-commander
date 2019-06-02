#!/usr/bin/env bash

port=11515

function usage() {
    echo "Usage: $0 [tsv|json|csv] <sql-string|@sql-file>"
    echo "tsv|json|csv: If the first argument is one of these, then the output will be accordingly. Default is tab-delimited."
    exit 1
}

if [[ $# == 0 ]] ;then
    usage
fi

dt=date
if [[ -x $(which gdate 2>/dev/null) ]] ; then
    dt=gdate
fi

accept="text/tab-separated-values"
if [[ $1 = json ]] ; then
    accept="application/json"
    shift
elif [[ $1 = csv ]] ; then
    accept="text/csv"
    shift
elif [[ $1 = tsv ]] ; then
    accept="text/tab-separated-values"
    shift
fi

function trim() {
    sed -e "s/^[[:space:]]*//;s/[[:space:]]*$//g"
}

function subst_vars() {
    local secs1970=$(${dt} -u +%s)
    local millis1970=$(${dt} -u +%s%N)
    local millis1970=${millis1970:0:13}
    sed -e 's/${secs1970}/'${secs1970}'/g;s/${millis1970}/'${millis1970}'/g'
}

function get_sql() {
    local sql=$(echo "$@"|trim)
    if [[ ${sql} =~ @.* ]] ; then
        local file=${sql:1}
        if [[ ! -f "$file" ]] ; then
            echo "File not found: $file"
            usage
        fi
        sql=$(cat ${file}|trim)
    fi
    echo "$sql"|subst_vars
}

sql=$(get_sql "$@")

path=update
if [[ ${sql^^} =~ SELECT[[:space:]]*.* ]] ; then
    path=select
else
    accept="application/json"
fi

curl -s -X POST -H "Content-Type: text/plain" -H "Accept: ${accept}" -d"${sql}" localhost:${port}/${path}
