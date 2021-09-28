#!/bin/bash

function prepare_submodules {
	git submodule update --init
	local SUBMODULE_LIST=`git submodule--helper status | awk '{print $2}'`
	while read -r submod; do
		local PARENT_DIR=`pwd`
		local BRANCH=`git submodule--helper remote-branch ${submod}`

		cd ${submod}
		git branch -f ${BRANCH} HEAD;
		git checkout ${BRANCH}
		if [ ! -z "`git submodule status`" ]; then
			prepare_submodules;
		fi
		cd ${PARENT_DIR}
	done <<< "$SUBMODULE_LIST"
}

prepare_submodules
git submodule update --rebase --recursive --remote