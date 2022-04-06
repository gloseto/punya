#!/bin/bash

/usr/lib/google-cloud-sdk/bin/java_dev_appserver.sh --port=8888 --address=0.0.0.0 /home/punya/appinventor/appengine/build/war/ &

cd /home/punya/appinventor/buildserver
ant RunLocalBuildServer