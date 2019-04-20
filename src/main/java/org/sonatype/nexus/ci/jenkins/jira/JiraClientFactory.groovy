/*
 * Copyright (c) 2018-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.nexus.ci.jenkins.jira

import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder
import hudson.security.ACL
import jenkins.model.Jenkins
import org.sonatype.nexus.ci.jenkins.config.NotifierConfiguration

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Preconditions.checkNotNull

class JiraClientFactory
{
  static JiraClient getJiraClient(String jobCredentialsId = null,
                                  PrintStream logger = null,
                                  final boolean verboseLogging = false,
                                  final boolean dryRun = false,
                                  final boolean disableJqlFieldFilter = false,
                                  final int jqlMaxResultsOverride = 50) {
    def configuration = NotifierConfiguration.getNotifierConfiguration()
    checkArgument(configuration != null, Messages.JiraClientFactory_NoConfiguration())
    checkArgument(configuration.jiraConfigs != null, Messages.JiraClientFactory_NoConfiguration())
    checkArgument(configuration.jiraConfigs.size() > 0, Messages.JiraClientFactory_NoConfiguration())

    def jiraConfig = configuration.jiraConfigs.get(0)

    def credentialsId = jobCredentialsId ?: jiraConfig.jiraCredentialsId

    def credentials = findCredentials(jiraConfig.jiraServerUrl, credentialsId)

    return new JiraClient(jiraConfig.jiraServerUrl, credentials.username, credentials.password.plainText, logger, verboseLogging, dryRun, disableJqlFieldFilter, jqlMaxResultsOverride)
  }

  static private StandardUsernamePasswordCredentials findCredentials(final String url, final String credentialsId) {
    checkNotNull(credentialsId)
    checkNotNull(url)
    checkArgument(!credentialsId.isEmpty())

    //noinspection GroovyAssignabilityCheck
    List<StandardUsernamePasswordCredentials> lookupCredentials = CredentialsProvider.lookupCredentials(
        StandardUsernamePasswordCredentials,
        Jenkins.getInstance(),
        ACL.SYSTEM,
        URIRequirementBuilder.fromUri(url).build())

    def credentials = CredentialsMatchers.firstOrNull(lookupCredentials, CredentialsMatchers.withId(credentialsId))
    checkArgument(credentials != null, Messages.JiraClientFactory_NoCredentials(credentialsId))
    return credentials
  }
}
