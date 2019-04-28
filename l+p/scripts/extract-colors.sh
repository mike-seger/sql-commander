#!/bin/bash

if [[ ! -d "$1" || ! -f "$1/index.html" ]] ; then
	echo "Usage: $0 <report dir>"
	exit 1
fi

grep -ri "#[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]" testresults/$dir | \
	sed -e 's/#/\n#/g;s/[^#0-9a-z]/\n/gI' | grep "#......"|sort -u | \
while read c ; do 
	echo ' <div style="background-color:'$c';">'$c'</div>'
done \
>target/t.html
