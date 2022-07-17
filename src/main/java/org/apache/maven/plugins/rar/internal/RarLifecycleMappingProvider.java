package org.apache.maven.plugins.rar.internal;

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

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;

/**
 * {@code rar} packaging plugins bindings provider for {@code default} lifecycle.
 */
@Singleton
@Named( "rar" )
public final class RarLifecycleMappingProvider
        implements Provider<LifecycleMapping>
{
    // Note: "this" plugin does NOT have to have version specified, as the version should be specified in
    // effective POM, otherwise this lifecycle mapping would not be loaded at all. Hence, the version of
    // "this" plugin (in this case maven-rar-plugin) version is NEVER considered, and will come from
    // effective POM of project using this plugin.
    @SuppressWarnings( "checkstyle:linelength" )
    private static final String[] BINDINGS =
            {
                    "process-resources", "org.apache.maven.plugins:maven-resources-plugin:3.2.0:resources",
                    "compile", "org.apache.maven.plugins:maven-compiler-plugin:3.8.1:compile",
                    "process-test-resources", "org.apache.maven.plugins:maven-resources-plugin:3.2.0:testResources",
                    "test-compile", "org.apache.maven.plugins:maven-compiler-plugin:3.8.1:testCompile",
                    "test", "org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5:test",
                    "package", "org.apache.maven.plugins:maven-rar-plugin:rar",
                    "install", "org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install",
                    "deploy", "org.apache.maven.plugins:maven-deploy-plugin:3.0.0-M1:deploy"
            };

    private final Lifecycle defaultLifecycle;
    private final LifecycleMapping lifecycleMapping;

    public RarLifecycleMappingProvider()
    {
        HashMap<String, LifecyclePhase> bindings = new HashMap<>();
        for ( int i = 0; i < BINDINGS.length; i = i + 2 )
        {
            bindings.put( BINDINGS[i], new LifecyclePhase( BINDINGS[i + 1] ) );
        }
        this.defaultLifecycle = new Lifecycle();
        this.defaultLifecycle.setId( "default" );
        this.defaultLifecycle.setLifecyclePhases( bindings );


        this.lifecycleMapping = new LifecycleMapping()
        {
            @Override
            public Map<String, Lifecycle> getLifecycles()
            {
                return Collections.singletonMap( "default", defaultLifecycle );
            }

            @Override
            public List<String> getOptionalMojos( String lifecycle )
            {
                return null;
            }

            @Override
            public Map<String, String> getPhases( String lifecycle )
            {
                if ( "default".equals( lifecycle ) )
                {
                    return defaultLifecycle.getPhases();
                }
                else
                {
                    return null;
                }
            }
        };
    }

    @Override
    public LifecycleMapping get()
    {
        return lifecycleMapping;
    }
}
