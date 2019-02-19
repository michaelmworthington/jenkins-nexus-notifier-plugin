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

import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import spock.lang.Ignore
import spock.lang.Specification

class JiraNotifierTest
    extends Specification
{
  def mockLogger = Mock(PrintStream)
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  JiraNotifier jiraNotifier

  def setup() {
    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]
    jiraNotifier = new JiraNotifier(mockRun, mockListener)
  }

  def 'send requires projectKey'() {
    when:
      jiraNotifier.send(true,
                        new JiraNotification(true,
                                             emptyOptions,
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             false,
                                             null),
                        null)

    then:
      IllegalArgumentException ex = thrown()
      ex.message == 'Jira Project Key is a required argument for the Jira Notifier'

    where:
      emptyOptions << [ null, '' ]
  }

  def 'creates Jira client with job credentials override'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)

    when:
      jiraNotifier.send(true,
                        new JiraNotification(true,
                                             'projectKey',
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             false,
                                             'overrideId'),
                        Mock(PolicyEvaluationHealthAction))

    then:
      1 * JiraClientFactory.getJiraClient('overrideId') >> client
  }

  def 'send expands notification arguments'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(JiraClient.class)
      IQClientFactory.getIQClient(_) >> iqClient

    when:
      mockRun.getEnvironment(_) >> ['projectKey': 'project']
      jiraNotifier = new JiraNotifier(mockRun, mockListener)
      jiraNotifier.send(true,
                        new JiraNotification(true,
                                             '${projectKey}',
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             false,
                                             null),
                        Mock(PolicyEvaluationHealthAction))

    then:
      1 * jiraClient.lookupJiraTickets(_) >> { arugments ->
        def policyEvaluationResult = arugments[0] as String
        assert policyEvaluationResult == 'project'
      }
  }

  @Ignore
  def 'putsCard to Jira client'() {
    setup:
      def policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
          reportLink: 'http://report.com/link',
          affectedComponentCount: 1,
          criticalComponentCount: 2,
          severeComponentCount: 3,
          moderateComponentCount: 5
        )
    def jiraNotification = new JiraNotification(true,
                                                'projectKey',
                                                true,
                                                true,
                                                "Done",
                                                "IQ Application",
                                                "IQ Organization",
                                                "Finding ID",
                                                "Security-High",
                                                false,
                                                null)
    GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(_) >> client

    when:
      jiraNotifier.send(buildPassing, jiraNotification, policyEvaluationHealthAction)

    then:
      1 * client.putCard(_) >> { arugments ->
        def policyEvaluationResult = arugments[0] as PolicyEvaluationResult
        assert policyEvaluationResult.projectKey == jiraNotification.projectKey
        assert policyEvaluationResult.repositorySlug == jiraNotification.repositorySlug
        assert policyEvaluationResult.commitHash == jiraNotification.commitHash
        assert policyEvaluationResult.buildStatus == (buildPassing ? BuildStatus.PASS : BuildStatus.FAIL)
        assert policyEvaluationResult.componentsAffected == policyEvaluationHealthAction.affectedComponentCount
        assert policyEvaluationResult.critical == policyEvaluationHealthAction.criticalComponentCount
        assert policyEvaluationResult.severe == policyEvaluationHealthAction.severeComponentCount
        assert policyEvaluationResult.moderate == policyEvaluationHealthAction.moderateComponentCount
        assert policyEvaluationResult.reportUrl == policyEvaluationHealthAction.reportLink
      }

    where:
      buildPassing << [true, false ]
  }
}
