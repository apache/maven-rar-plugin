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

File rarFile = new File( basedir, "target/rar-dependencies-1.0-SNAPSHOT.rar" )
assert rarFile.isFile() : "rar file not found: " + rarFile

JarFile rar = new JarFile( rarFile )
try
{
    Set<String> jars = new TreeSet<String>()
    for ( entry in rar.entries() )
    {
        if ( entry.name.endsWith( ".jar" ) )
        {
            jars.add( entry.name )
        }
    }

    // compile is copied; optional (excluded by RarMojo), provided and test (outside runtime scope) are not
    assert jars == ( [ "javax.inject-1.jar" ] as Set ) : "unexpected jars in rar: " + jars
}
finally
{
    rar.close()
}
