#!/bin/bash -e
#*******************************************************************************
# Copyright (c) 2009, 2010 Tasktop Technologies and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#      Tasktop Technologies - initial API and implementation
#*******************************************************************************

help() {
  echo "usage: sign-update-site.sh major build"
  echo "                           local"
  exit 1
}

if [ $# -eq 0 ]
then
 help
fi

if [ "$1" == "local" ]
then
 BUILD_ROOT=$(cd $(dirname $0); pwd)
 source $BUILD_ROOT/local.sh
else
 if [ $# -lt 2 ]
 then
  help
 fi

 MAJOR_VERSION=$1
 QUALIFIER=$2
fi

SRC=/home/data/httpd/download.eclipse.org/tools/mylyn/update-archive/$MAJOR_VERSION/$QUALIFIER
DST=/opt/public/download-staging.priv/tools/mylyn
OUT=$DST/output
TMP=$DST/tmp/$MAJOR_VERSION-$QUALIFIER
JAVA_HOME=/opt/ibm/java2-ppc-50
ECLIPSE_HOME=/shared/tools/mylyn/eclipse

unzip() {
 /bin/rm -R $TMP/$1 || true
 /bin/mkdir -p $TMP/$1
 /usr/bin/unzip -d $TMP/$1 $SRC/mylyn-$MAJOR_VERSION.$QUALIFIER-$1.zip
}

rezip() {
 cd $TMP/$1
 echo Rezipping archive for $1, output is logged to $DST/sign.log
 /usr/bin/zip $TMP/mylyn-$MAJOR_VERSION.$QUALIFIER-$1.zip -r . >> $DST/sign.log
}

pack() {
DIR=$TMP/$1
$JAVA_HOME/bin/java \
 -Xmx512m \
 -jar $ECLIPSE_HOME/plugins/org.eclipse.equinox.launcher_*.jar \
 -application org.eclipse.update.core.siteOptimizer \
 -jarProcessor -verbose -processAll -repack -pack \
 -digestBuilder -digestOutputDir=$DIR -siteXML=$DIR/site.xml \
 -outputDir $DIR $DIR

echo Processing $DIR
rm -f $DIR/artifacts.jar $DIR/content.jar
$JAVA_HOME/bin/java \
 -Xmx512m \
 -jar $ECLIPSE_HOME/plugins/org.eclipse.equinox.launcher_*.jar \
 -application org.eclipse.equinox.p2.metadata.generator.EclipseGenerator \
 -updateSite $DIR \
 -site file:$DIR/site.xml \
 -metadataRepository file:$DIR \
 -metadataRepositoryName "$2 "\
 -artifactRepository file:$DIR \
 -artifactRepositoryName "$2" \
 -compress \
 -reusePack200Files \
 -noDefaultIUs
}

if [ ! -e $TMP ]
then

 # extract site

 /bin/rm $TMP || true
 unzip e3.3
 unzip e3.4
 unzip extras
 unzip incubator

 echo Creating archive for singing, output is logged to $DST/sign.log
 /bin/rm $DST/mylyn.zip || true
 cd $TMP
 /usr/bin/find -name "org.eclipse.mylyn*.jar" | zip $DST/mylyn.zip -@ > $DST/sign.log

 # sign

 mkdir -p $OUT
 /bin/rm $OUT/mylyn.zip || true
 /usr/bin/sign $DST/mylyn.zip nomail $OUT
fi

I=0
while [ $I -lt 30 ] && [ ! -e $OUT/mylyn.zip ]; do
  echo Waiting for $OUT/mylyn.zip
  sleep 30
  let I=I+1
done

if [ ! -e $OUT/mylyn.zip ]
then
  echo
  echo Signing Failed: Timeout waiting for $OUT/mylyn.zip
  exit 1
fi

# repack site

echo Unzipping signed files, output is logged to $DST/sign.log
/usr/bin/unzip -o -d $TMP $OUT/mylyn.zip >> $DST/sign.log

pack e3.3 "Mylyn for Eclipse 3.3"
pack e3.4 "Mylyn for Eclipse 3.4 and 3.5"
pack extras "Mylyn Extras"
pack incubator "Mylyn Incubator"

rezip e3.3
rezip e3.4
rezip extras
rezip incubator

# republish

/bin/mv $SRC $SRC-DELETE
# other zip files are overridden here with signed versions
/bin/cp -av $TMP $SRC
# recover wikitext-standalone zip and other archives
/bin/cp -v $SRC-DELETE/mylyn-wikitext-standalone-*.zip $SRC
/bin/chgrp -R mylynadmin $SRC
/bin/chmod g+w -R $SRC
/bin/chmod o+r -R $SRC
/usr/bin/find $SRC -type d | xargs chmod +x
rm -R $SRC-DELETE
