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
package org.apache.maven.plugins.rar.stubs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 */
public class RarMavenProjectStub extends MavenProject {
    private List<Artifact> attachedArtifacts;

    public RarMavenProjectStub() {
        super(new Model());

        super.setGroupId(getGroupId());
        super.setArtifactId(getArtifactId());
        super.setVersion(getVersion());
        super.setDescription("Test description");

        Organization org = new Organization();
        org.setName("organization");
        org.setUrl("http://www.some.org");

        super.setOrganization(org);
        super.setFile(getFile());
        super.setArtifact(getArtifact());
        super.setRemoteArtifactRepositories(Collections.emptyList());
        super.setPluginArtifactRepositories(Collections.emptyList());
        super.setCollectedProjects(Collections.emptyList());
        super.setActiveProfiles(Collections.emptyList());

        super.addCompileSourceRoot(getBasedir() + "/src/test/resources/unit/basic-rar-test/src/main/java");
        super.addTestCompileSourceRoot(getBasedir() + "/src/test/resources/unit/basic-rar-test/src/test/java");

        super.setExecutionRoot(false);

        Build build = new Build();
        build.setDirectory(getBasedir() + "/target");
        build.setSourceDirectory(getBasedir() + "/src/main/java");
        build.setOutputDirectory(getBasedir() + "/target/classes");
        build.setTestSourceDirectory(getBasedir() + "/src/test/java");
        build.setTestOutputDirectory(getBasedir() + "/target/test-classes");
        setBuild(build);
    }

    public String getGroupId() {
        return "org.apache.maven.test";
    }

    public String getArtifactId() {
        return "maven-rar-test";
    }

    public String getVersion() {
        return "1.0-SNAPSHOT";
    }

    public File getFile() {
        return new File(getBasedir(), "src/test/resources/unit/basic-rar-test/plugin-config.xml");
    }

    public File getBasedir() {
        return new File(PlexusTestCase.getBasedir());
    }

    public Artifact getArtifact() {
        Artifact artifact = new RarArtifactStub();

        artifact.setGroupId(getGroupId());

        artifact.setArtifactId(getArtifactId());

        artifact.setVersion(getVersion());

        return artifact;
    }

    public Set<Artifact> getArtifacts() {
        Set<Artifact> artifacts = new HashSet<>();

        artifacts.add(createArtifact("org.apache.maven.test", "maven-artifact01", "1.0-SNAPSHOT", false));
        artifacts.add(createArtifact("org.apache.maven.test", "maven-artifact02", "1.0-SNAPSHOT", false));

        return artifacts;
    }

    public List<Artifact> getAttachedArtifacts() {
        if (attachedArtifacts == null) {
            attachedArtifacts = new ArrayList<>();
        }
        return attachedArtifacts;
    }

    protected Artifact createArtifact(String groupId, String artifactId, String version, boolean optional) {
        Artifact artifact = new RarArtifactStub();

        artifact.setGroupId(groupId);

        artifact.setArtifactId(artifactId);

        artifact.setVersion(version);

        artifact.setOptional(optional);

        artifact.setVersionRange(VersionRange.createFromVersion("1.0"));

        artifact.setFile(new File(
                getBasedir() + "/src/test/remote-repo/" + artifact.getGroupId().replace('.', '/') + "/"
                        + artifact.getArtifactId() + "/" + artifact.getVersion() + "/"
                        + artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar"));

        artifact.setArtifactHandler(new ArtifactHandlerStub());
        return artifact;
    }
}
