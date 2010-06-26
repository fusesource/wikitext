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

if [ $# -eq 0 ]
then
 echo "usage: get-orbit-entries.sh url"
 exit 1
fi

BUILD_ROOT=$(cd $(dirname $0)/..; pwd)
MAP=$BUILD_ROOT/maps/orbit.map
TMPMAP=$BUILD_ROOT/orbit.map
TMP=$BUILD_ROOT/orbit-all.map

wget $1 -O $TMP

grep "^plugin@com.sun.syndication,0.9.0" $TMP > $TMPMAP
grep "^plugin@com.sun.xml.bind,2.2.0" $TMP > $TMPMAP
grep "^plugin@javax.activation,1.1.0" $TMP >> $TMPMAP
grep "^plugin@javax.mail,1.4.0" $TMP >> $TMPMAP
grep "^plugin@javax.servlet,2.4.0" $TMP >> $TMPMAP
grep "^plugin@javax.xml.bind,2.3.0" $TMP >> $TMPMAP
grep "^plugin@javax.xml.rpc,1.1.0" $TMP >> $TMPMAP
grep "^plugin@javax.xml.soap,1.2.0" $TMP >> $TMPMAP
grep "^plugin@javax.wsdl,1.6.2" $TMP >> $TMPMAP
grep "^plugin@org.apache.ant,1.7.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.axis,1.4.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.commons.codec,1.3.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.commons.discovery,0.2.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.commons.httpclient,3.1.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.commons.lang,2.3.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.commons.logging,1.0.4" $TMP >> $TMPMAP
grep "^plugin@org.apache.xmlrpc,3.0.0" $TMP >> $TMPMAP
grep "^plugin@org.apache.ws.commons.util,1.0.0" $TMP >> $TMPMAP
grep "^plugin@org.jdom,1.0.0" $TMP >> $TMPMAP

rm $TMP
mv $TMPMAP $MAP

echo $MAP sucessfully updated