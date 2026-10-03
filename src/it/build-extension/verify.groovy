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

def version = (new File(basedir, "pom.xml").text =~ /<artifactId>maven-rar-plugin<\/artifactId>\s*<version>([^<]+)<\/version>/)[0][1]
def log = new File(basedir, "build.log").text

// The lifecycle mapping must bind the rar goal to the loaded extension's version,
// not to whatever version repository metadata resolves for a version-less binding.
if (!log.contains(":" + version + ":rar (default-rar)")) {
    throw new AssertionError("rar goal did not run at extension version " + version)
}
assert new File(basedir, "target/build-extension-1.0-SNAPSHOT.rar").isFile()
