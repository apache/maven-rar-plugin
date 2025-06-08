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
package org.apache.maven.plugins.rar;

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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Builds J2EE Resource Adapter Archive (RAR) files.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 */
@Mojo(
        name = "rar",
        threadSafe = true,
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.TEST)
public class RarMojo extends AbstractMojo {
    private static final String RA_XML_URI = "META-INF/ra.xml";

    /**
     * Single directory for extra files to include in the RAR.
     */
    @Parameter(defaultValue = "${basedir}/src/main/rar", required = true)
    private File rarSourceDirectory;

    /**
     * The location of the ra.xml file to be used within the rar file.
     */
    @Parameter(defaultValue = "${basedir}/src/main/rar/META-INF/ra.xml")
    private File raXmlFile;

    /**
     * Specify if the generated jar file of this project should be
     * included in the rar file ; default is true.
     */
    @Parameter(defaultValue = "true")
    private Boolean includeJar;

    /**
     * The location of the manifest file to be used within the rar file.
     */
    @Parameter(defaultValue = "${basedir}/src/main/rar/META-INF/MANIFEST.MF")
    private File manifestFile;

    /**
     * Directory that resources are copied to during the build.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}", required = true)
    private String workDirectory;

    /**
     * The directory for the generated RAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    /**
     * The name of the RAR file to generate.
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true, readonly = true)
    private String finalName;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be attached.
     *
     * If this is not given, it will merely be written to the output directory
     * according to the finalName.
     *
     * @since 2.4
     */
    @Parameter(property = "maven.rar.classifier", defaultValue = "")
    private String classifier;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * allow filtering of link{rarSourceDirectory}
     *
     * @since 2.3
     */
    @Parameter(property = "maven.rar.filterRarSourceDirectory", defaultValue = "false")
    private boolean filterRarSourceDirectory;

    /**
     * @since 2.3
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    /**
     * @since 2.3
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    /**
     * Whether to escape backslashes and colons in windows-style paths.
     *
     * @since 2.3
     */
    @Parameter(property = "maven.resources.escapeWindowsPaths", defaultValue = "true")
    protected boolean escapeWindowsPaths;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @since 2.3
     */
    @Parameter(property = "maven.resources.escapeString")
    protected String escapeString;

    /**
     * Overwrite existing files even if the destination files are newer.
     *
     * @since 2.3
     */
    @Parameter(property = "maven.resources.overwrite", defaultValue = "false")
    private boolean overwrite;

    /**
     * Copy any empty directories included in the Resources.
     *
     * @since 2.3
     */
    @Parameter(property = "maven.resources.includeEmptyDirs", defaultValue = "false")
    protected boolean includeEmptyDirs;

    /**
     * stop searching endToken at the end of line
     *
     * @since 2.3
     */
    @Parameter(property = "maven.resources.supportMultiLineFiltering", defaultValue = "false")
    private boolean supportMultiLineFiltering;

    /**
     * @since 2.3
     */
    @Parameter(defaultValue = "true")
    protected boolean useDefaultDelimiters;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the
     * form 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p><p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt;/delimiter&gt;
     *   &lt;delimiter&gt;@&lt;/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     *
     * @since 2.3
     */
    @Parameter
    protected LinkedHashSet<String> delimiters;

    /**
     * <p>
     * The list of extra filter properties files to be used along with System properties,
     * project properties, and filter properties files specified in the POM build/filters section,
     * which should be used for the filtering during the current mojo execution.
     * </p>
     * <p>
     * Normally, these will be configured from a plugin's execution section, to provide a different
     * set of filters for a particular execution. For instance, starting in Maven 2.2.0, you have the
     * option of configuring executions with the id's <code>default-resources</code> and
     * <code>default-testResources</code> to supply different configurations for the two
     * different types of resources. By supplying <code>extraFilters</code> configurations, you
     * can separate which filters are used for which type of resource.
     * </p>
     *
     * @since 2.3
     */
    @Parameter
    protected List<String> filters;

    /**
     * Additional file extensions to not apply filtering (already defined are : jpg, jpeg, gif, bmp, png)
     *
     * @since 2.3
     */
    @Parameter
    protected List<String> nonFilteredFileExtensions;

    /**
     * extra resource to include in rar archive
     *
     * @since 2.3
     */
    @Parameter
    protected List<RarResource> rarResources;

    /**
     * Whether to warn if the <code>ra.xml</code> file is missing. Set to <code>false</code>
     * if you want you RAR built without a <code>ra.xml</code> file.
     * This may be useful if you are building against JCA 1.6 or later.
     *
     * @since 2.3
     */
    @Parameter(property = "maven.rar.warnOnMissingRaXml", defaultValue = "true")
    protected boolean warnOnMissingRaXml;

    /**
     * To skip execution of the rar mojo.
     *
     * @since 2.4
     */
    @Parameter(property = "maven.rar.skip")
    private boolean skip;

    /**
     * The maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Timestamp for reproducible output archive entries, either formatted as ISO 8601
     * <code>yyyy-MM-dd'T'HH:mm:ssXXX</code> or as an int representing seconds since the epoch (like
     * <a href="https://reproducible-builds.org/docs/source-date-epoch/">SOURCE_DATE_EPOCH</a>).
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "${project.build.outputTimestamp}")
    private String outputTimestamp;

    /**
     * The Jar archiver.
     */
    private final JarArchiver jarArchiver;

    /**
     * @since 2.3
     */
    protected final MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @since 2.4
     */
    private final MavenProjectHelper projectHelper;

    private File buildDir;

    @Inject
    public RarMojo(
            JarArchiver jarArchiver,
            MavenResourcesFiltering mavenResourcesFiltering,
            MavenProjectHelper projectHelper) {
        this.jarArchiver = jarArchiver;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.projectHelper = projectHelper;
    }

    /** {@inheritDoc} */
    public void execute() throws MojoExecutionException {

        if (skip) {
            getLog().info("Skipping rar generation.");
            return;
        }

        // Check if jar file is there and if requested, copy it
        try {
            if (includeJar) {
                File generatedJarFile = new File(outputDirectory, finalName + ".jar");
                if (generatedJarFile.exists()) {
                    getLog().info("Including generated jar file[" + generatedJarFile.getName() + "]");
                    FileUtils.copyFileToDirectory(generatedJarFile, getBuildDir());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying generated Jar file", e);
        }

        // Copy dependencies
        try {
            Set<Artifact> artifacts = project.getArtifacts();
            for (Artifact artifact : artifacts) {

                ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
                if (!artifact.isOptional()
                        && filter.include(artifact)
                        && artifact.getArtifactHandler().isAddedToClasspath()) {
                    getLog().info("Copying artifact[" + artifact.getGroupId() + ", " + artifact.getId() + ", "
                            + artifact.getScope() + "]");
                    FileUtils.copyFileToDirectory(artifact.getFile(), getBuildDir());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying RAR dependencies", e);
        }

        resourceHandling();

        // Include custom manifest if necessary
        try {
            includeCustomRaXmlFile();
        } catch (IOException e) {
            throw new MojoExecutionException("Error copying ra.xml file", e);
        }

        // Check if connector deployment descriptor is there
        File ddFile = new File(getBuildDir(), RA_XML_URI);
        if (!ddFile.exists() && warnOnMissingRaXml) {
            getLog().warn("Connector deployment descriptor: " + ddFile.getAbsolutePath() + " does not exist.");
        }

        File rarFile = getRarFile(outputDirectory, finalName, classifier);
        MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setCreatedBy("Maven RAR Plugin", "org.apache.maven.plugins", "maven-rar-plugin");
        archiver.setOutputFile(rarFile);

        // configure for Reproducible Builds based on outputTimestamp value
        archiver.configureReproducible(outputTimestamp);

        try {
            // Include custom manifest if necessary
            includeCustomManifestFile();

            archiver.getArchiver().addDirectory(getBuildDir());
            archiver.createArchive(session, project, archive);
        } catch (IOException | ManifestException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error assembling RAR", e);
        }

        if (classifier != null) {
            projectHelper.attachArtifact(project, "rar", classifier, rarFile);
        } else {
            project.getArtifact().setFile(rarFile);
        }
    }

    private void resourceHandling() throws MojoExecutionException {
        Resource resource = new Resource();
        resource.setDirectory(rarSourceDirectory.getAbsolutePath());
        resource.setTargetPath(getBuildDir().getAbsolutePath());
        resource.setFiltering(filterRarSourceDirectory);

        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        if (rarResources != null && !rarResources.isEmpty()) {
            resources.addAll(rarResources);
        }

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
                resources, getBuildDir(), project, encoding, filters, Collections.<String>emptyList(), session);

        mavenResourcesExecution.setEscapeWindowsPaths(escapeWindowsPaths);

        // never include project build filters in this call, since we've already accounted for the POM build filters
        // above, in getCombinedFiltersList().
        mavenResourcesExecution.setInjectProjectBuildFilters(false);

        mavenResourcesExecution.setEscapeString(escapeString);
        mavenResourcesExecution.setOverwrite(overwrite);
        mavenResourcesExecution.setIncludeEmptyDirs(includeEmptyDirs);
        mavenResourcesExecution.setSupportMultiLineFiltering(supportMultiLineFiltering);
        mavenResourcesExecution.setDelimiters(delimiters, useDefaultDelimiters);

        if (nonFilteredFileExtensions != null) {
            mavenResourcesExecution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
        }
        try {
            mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Error copying RAR resources", e);
        }
    }

    /**
     * @return The buildDir.
     */
    protected File getBuildDir() {
        if (buildDir == null) {
            buildDir = new File(workDirectory);
        }
        return buildDir;
    }

    /**
     * @param basedir The basedir.
     * @param finalName The finalName.
     * @param classifier The classifier.
     * @return the resulting file which contains classifier.
     */
    protected static File getRarFile(File basedir, String finalName, String classifier) {
        if (classifier == null) {
            classifier = "";
        } else if (!classifier.trim().isEmpty() && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(basedir, finalName + classifier + ".rar");
    }

    private void includeCustomManifestFile() throws IOException {
        File customManifestFile = manifestFile;
        if (!customManifestFile.exists()) {
            getLog().info("Could not find manifest file: " + manifestFile + " - Generating one");
        } else {
            getLog().info("Including custom manifest file[" + customManifestFile + "]");
            archive.setManifestFile(customManifestFile);
            File metaInfDir = new File(getBuildDir(), "META-INF");
            FileUtils.copyFileToDirectory(customManifestFile, metaInfDir);
        }
    }

    private void includeCustomRaXmlFile() throws IOException {
        if (raXmlFile == null) {
            return;
        }
        File raXml = raXmlFile;
        if (raXml.exists()) {
            getLog().info("Using ra.xml " + raXmlFile);
            File metaInfDir = new File(getBuildDir(), "META-INF");
            FileUtils.copyFileToDirectory(raXml, metaInfDir);
        }
    }
}
