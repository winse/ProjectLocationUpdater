#!/bin/sh

if [ $# != 2 ] ; then
	echo "USAGE: $0 old_workspace_windows_absolute_path new_workspace_relative_or_linux_path"
	echo " e.g.: $0 C:/bigendian/big0 ../frameworks/bigendian/big0"
	exit 1;
fi


old_workspace_loc=$1
new_workspace_loc=$2

# 需为绝对路径
APP_DIR="$(cd $(dirname $0); pwd)"

cd $new_workspace_loc
java.exe -jar `wslpath -w $APP_DIR/eclipse-workspace-migration.jar` $old_workspace_loc $(ls .metadata/.plugins/org.eclipse.core.resources/.projects/ | xargs echo )

