#!/bin/bash

# root of build tree
BUILD_ROOT=$(cd $(dirname $0); pwd)

source $BUILD_ROOT/local.sh

cp 3.4/build/allUpdateSite/plugins/*jar $ECLIPSE_TEST_HOME_3_4/plugins/

rm -R $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests
mkdir $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests
unzip -o $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests_0.0.0.jar \
 -d $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests
rm $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests_0.0.0.jar

$JAVA_HOME/bin/java \
 -jar $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.equinox.launcher_*.jar \
 -clean \
 -application org.eclipse.ant.core.antRunner \
 -file $ECLIPSE_TEST_HOME_3_4/plugins/org.eclipse.mylyn.tests/test.xml \
 -Declipse-home=$ECLIPSE_TEST_HOME_3_4 \
 "-DextraVMargs=-Declipse.perf.dbloc=$BUILD_ROOT/derby \
                -Declipse.perf.config=build=$QUALIFIER;config=$HOST-3.4;jvm=$JVM" \
 -Dos=linux -Dws=gtk -Darch=x86 \
 "-Dvmargs=-Xms256M -Xmx256M" \
 -logger org.apache.tools.ant.DefaultLogger \
 $@
