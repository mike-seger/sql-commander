#!/bin/bash

if [[ ! -d "$1" || ! -f "$1/index.html" ]] ; then
	echo "Usage: $0 <report dir>"
	exit 1
fi

mysed=sed
if [[ -x "$(which gsed)" ]] ; then
    mysed=gsed
fi


report_dir="$1"

colors=(
'#C73905/#0539C7'
'#CF6900/#0069CF'
'#D11C97/#971CD1'
'#E37400/#0074E3'
'#FF0000/#0000FF'
'#FF9916/#1699FF'
'#FF9D00/#009DFF'
'#FF9E2A/#2A9EFF'
'#D16B00/#006BD1'
'#F15C80/#805CF1'
'#F45B5B/#5B5BF4'
'#F7A35C/#5CA3F7'
'#FF9916/#1699FF'
)

sedexp=""
grepexp="NOMATCH"
for c in "${colors[@]}" ; do
	grepexp="${grepexp}|${c/\/*/}"
	sedexp="${sedexp}s/${c}/gI;"
done

echo "$sedexp"
echo "($grepexp)"

egrep -ril "($grepexp)" "$report_dir" | while read f ; do
    $mysed -i "$sedexp" "${f}"
done

cp $(dirname "$0")/stat-fond.png "$report_dir/style/stat-fond.png"
