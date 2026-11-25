/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.jar.JarFile

File jarFile = new File( basedir, "target/maven-it-non-filtered-1.0-SNAPSHOT.rar" )
println "Checking for existence of " + jarFile
assert jarFile.isFile() : "rar file not found"

JarFile jar = new JarFile( jarFile )

String[] includedEntries = [
    "META-INF/ra.xml",
    "SomeResource.txt",
    "ext/wine.txt",
    "ext-filtered/wine.txt"
]
for ( String included : includedEntries )
{
    println "Checking for existence of " + included
    assert jar.getEntry( included ) != null : "entry not in rar file" + included
}

String content = new String( jar.getInputStream( jar.getEntry( "SomeResource.txt" ) ).bytes )
int idx = content.indexOf( "1.0-SNAPSHOT" )
assert idx >= 1 : "SomeResource.txt not filtered"

content = new String( jar.getInputStream( jar.getEntry( "ext/wine.txt" ) ).bytes )
idx = content.indexOf( '${bestwine}' )
assert idx >= 1 : "ext/wine.txt filtered:" + content

content = new String( jar.getInputStream( jar.getEntry( "ext-filtered/wine.txt" ) ).bytes )
idx = content.indexOf( "bordeaux" )
assert idx >= 1 : "ext-filtered/wine.txt filtered:" + content

jar.close()
