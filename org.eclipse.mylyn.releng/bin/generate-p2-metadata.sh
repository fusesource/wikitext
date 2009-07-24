#*******************************************************************************
# Copyright (c) 2009 Tasktop Technologies and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#      Tasktop Technologies - initial API and implementation
#*******************************************************************************
#!/bin/bash -e

if [ $# -lt 1 ]
then
 ROOT=$HOME/downloads/tools/mylyn/update 
else
 ROOT=$1
fi

JAVA_HOME=/opt/ibm/java2-ppc-50
ECLIPSE_HOME=/shared/tools/mylyn/eclipse

pack() {
DIR=$ROOT/$1
echo Processing $DIR
rm -f $DIR/artifacts.jar $DIR/content.jar $DIR/digest.zip

$JAVA_HOME/bin/java \
 -Xmx512m \
 -jar $ECLIPSE_HOME/plugins/org.eclipse.equinox.launcher_*.jar \
 -application org.eclipse.update.core.siteOptimizer \
 -verbose -processAll \
 -digestBuilder -digestOutputDir=$DIR -siteXML=$DIR/site.xml
 
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

chmod 664 $DIR/artifacts.jar $DIR/content.jar $DIR/digest.zip
}

pack e3.3 "Mylyn for Eclipse 3.3"
pack e3.4 "Mylyn for Eclipse 3.4 and 3.5"
pack extras "Mylyn Extras"
pack incubator "Mylyn Incubator"

echo Done
