# check for build/shipkit and clone if not there, this should come first
SHIPKIT_DIR = build/shipkit
$(shell [ ! -e $(SHIPKIT_DIR) ] && git clone -b v1.0.46 https://github.com/yakworks/shipkit.git $(SHIPKIT_DIR) >/dev/null 2>&1)
# Shipkit.make first, which does all the lifting to create makefile.env for the BUILD_VARS
include $(SHIPKIT_DIR)/Shipkit.make
include $(SHIPKIT_DIR)/makefiles/vault.make
include $(SHIPKIT_MAKEFILES)/git-tools.make
include $(SHIPKIT_MAKEFILES)/gradle-tools.make
include $(SHIPKIT_MAKEFILES)/ship-version.make
include $(SHIPKIT_MAKEFILES)/circle.make

# should run vault.decrypt before this,
# sets up github, kubernetes and docker login
ship.authorize: git.config-bot-user
	$(logr.done)

publish.gradle-lib:
	if [ "$(IS_SNAPSHOT)" ]; then
		$(logr) "SNAPSHOT so not publishing"
	else
		$(logr) "publishing to gradle portal"
		$(gradlew) publishPlugins
	fi

ifdef RELEASABLE_BRANCH_OR_DRY_RUN

 ship.release: build publish.gradle-lib
	$(logr.done)

else

 ship.release:
	$(logr.done) "not on a RELEASABLE_BRANCH, nothing to do"

endif # end RELEASABLE_BRANCH
