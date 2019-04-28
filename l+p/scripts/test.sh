#!/usr/bin/env bash

set -e

cd $(dirname "$0")/..

tmpdir=$(mktemp -d "${TMPDIR:-/tmp/}$(basename $0).XXXXXXXXXXXX")
trap "rm -rf '$tmpdir'" EXIT

#scripts/init.sh
scripts/runSimulation.sh target/lp-test-sqlcommander-0.0.1.jar \
    http://localhost:11515 sqlcommander.SelectSimulation 100 |tee out.txt

tok="Please open the following file:"
index=$(grep "${tok}" out.txt | sed -e "s/${tok}//;s/^ *//;s/ *$//")
report_dir=$(dirname ${index})

scripts/beautify-gatling.sh "${report_dir}"

echo "Reports beautified"
