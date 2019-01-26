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

import hudson.AbortException
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

import javax.annotation.Nonnull
import java.security.MessageDigest

import static com.google.common.base.Preconditions.checkArgument
import static com.google.common.base.Strings.isNullOrEmpty

class JiraNotifier
{
  final Run run
  final TaskListener listener
  final PrintStream logger

  JiraNotifier(@Nonnull final Run run, @Nonnull final TaskListener listener) {
    this.run = run
    this.listener = listener
    this.logger = listener.logger
  }

  void send(final boolean buildPassing,
            final JiraNotification jiraNotification,
            final PolicyEvaluationHealthAction policyEvaluationHealthAction)
  {
    checkArgument(!isNullOrEmpty(jiraNotification.projectKey), Messages.JiraNotifier_NoProjectKey())

    def iqClient = IQClientFactory.getIQClient(jiraNotification.jobCredentialsId) //TODO: create separate credentials
    def jiraClient = JiraClientFactory.getJiraClient(jiraNotification.jobCredentialsId)

    def envVars = run.getEnvironment(listener)
    def projectKey = envVars.expand(jiraNotification.projectKey)

    logger.println("Creating Jira Tickets for Project: " + projectKey);

    sendPolicyEvaluationHealthAction(iqClient,
                                     jiraClient,
                                     projectKey,
                                     buildPassing,
                                     PolicyEvaluationHealthAction.build(policyEvaluationHealthAction))
  }

  private void sendPolicyEvaluationHealthAction(final IQClient iqClient,
                                                final JiraClient jiraClient,
                                                final String projectKey,
                                                final boolean buildPassing,
                                                final PolicyEvaluationHealthAction policyEvaluationHealthAction)
  {
    try {
      // 1. Get Policy Findings from IQ
      Set potentialFindings = iqClient.lookupPolcyDetailsFromIQ("aaaaaaa-testidegrandfathering", //TODO: parse the app name, is this just in the report link???
              "a22d44d0209b47358c8dd2532bb7afb3");
      System.out.println("IQ Link: " + policyEvaluationHealthAction.reportLink);

      // 2. Get Tickets from Jira
      def currentFindings = jiraClient.lookupJiraTickets(projectKey);

      // 3. Filter out Existing Tickets
      System.out.println("Potential Findings: " + potentialFindings.size());
      System.out.println("Current Findings: " + currentFindings.issues.size);

      // 4. Create New Tickets
      def description = "Sonatype IQ Server SECURITY-HIGH Policy Violation"
      def detail = "CVE-2019-1234"
      def source = "SonatypeIQ:IQServerAppId:scanIQ"
      def severity = 1
      def fprint = fingerprint("SONATYPEIQ-APPID-COMPONENTID-SVCODE")


      jiraClient.createIssue(projectKey,
                             description,
                             detail,
                             source,
                             severity,
                             fprint)

      // 5. Close fixed Tickets
      String ticketInternalId = 10210; //todo: close the right ones

      jiraClient.closeTicket(ticketInternalId);

    } catch (ex) {
      logger.println(ex.message)
      throw new AbortException(ex.message)
    }
  }

  private String fingerprint(def s)
  {
    // not until 2.5 = http://mrhaki.blogspot.com/2018/06/groovy-goodness-calculate-md5-and-sha.html
    //s.digest('SHA-256')

    MessageDigest.getInstance("SHA-256").digest(s.bytes).encodeHex().toString()
  }
}
