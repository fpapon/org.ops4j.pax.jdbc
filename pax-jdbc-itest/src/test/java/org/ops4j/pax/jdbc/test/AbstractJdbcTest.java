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
package org.ops4j.pax.jdbc.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;

/**
 * Base class for all integration tests - manually sets up pax-exam configuration (without implicit configuration).
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public abstract class AbstractJdbcTest {

    private static Logger logger = LoggerFactory.getLogger(AbstractJdbcTest.class);

    @Rule
    public TestName testName = new TestName();

    @Inject
    protected BundleContext context;

    @Before
    public void beforeEach() {
        logger.info("========== Running {}.{}() ==========", getClass().getName(), testName.getMethodName());
    }

    @After
    public void afterEach() {
        logger.info("========== Finished {}.{}() ==========", getClass().getName(), testName.getMethodName());
    }

    protected void assertAllBundlesResolved() {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getState() == Bundle.INSTALLED) {
                // Provoke exception
                try {
                    bundle.start();
                } catch (BundleException e) {
                    Assert.fail(e.getMessage());
                }
            }
        }
    }

    protected MavenArtifactProvisionOption mvnBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId).versionAsInProject();
    }

    protected Bundle getBundle(String symbolicName) {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

    public Option regressionDefaults() {
        return composite(
                systemTimeout(60 * 60 * 1000),

                // set to "4" to see Felix wiring information
                frameworkProperty("felix.log.level").value("1"),

                // added implicitly by pax-exam, if pax.exam.system=test
                // these resources are provided inside org.ops4j.pax.exam:pax-exam-link-mvn jar
                // for example, "link:classpath:META-INF/links/org.ops4j.base.link" = "mvn:org.ops4j.base/ops4j-base/1.5.0"
                url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.tracker.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                linkBundle("org.apache.servicemix.bundles.javax-inject").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                linkBundle("org.ops4j.pax.logging.pax-logging-api").startLevel(START_LEVEL_SYSTEM_BUNDLES),
                linkBundle("org.ops4j.pax.logging.pax-logging-log4j2").startLevel(START_LEVEL_SYSTEM_BUNDLES),

                junitBundles(),

                // org.ops4j.pax.exam.nat.internal.NativeTestContainer.start() adds this explicitly
                systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),

                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),

//                systemProperty("osgi.console").value("6666"),

                mvnBundle("org.osgi", "org.osgi.service.jdbc").versionAsInProject(),
                mvnBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject()
        );
    }

    public Option poolDefaults() {
        return composite(
                systemPackage("javax.transaction;version=1.1.0"),
                systemPackage("javax.transaction.xa;version=1.1.0"),
                mvnBundle("org.ops4j.pax.jdbc", "pax-jdbc-pool-common").versionAsInProject(),
                mvnBundle("org.apache.geronimo.specs", "geronimo-validation_1.0_spec").versionAsInProject(),
                mvnBundle("org.apache.aries", "org.apache.aries.util").versionAsInProject(),
                mvnBundle("org.apache.aries.transaction", "org.apache.aries.transaction.manager").versionAsInProject()
        );
    }

}
