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
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil
import spock.lang.Ignore
import spock.lang.Specification

class JiraNotifierTest
    extends Specification
{
  //private static final String jiraPort = "59454" //for Charles Proxy
  private static final String jiraPort = "8080"
  //private static final String iqPort = "60359" //for Charles Proxy
  private static final String iqPort = "8060"

  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
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
                                           false,
                                           emptyOptions,
                                           null,
                                           null,
                                           null,
                                           false,
                                           false,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           false,
                                           false,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
                                           null,
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
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

      PolicyEvaluationHealthAction pehaMock  = Mock(PolicyEvaluationHealthAction)
      pehaMock.getReportLink() >> "http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/6aeee85d9f8d45abbd91859352742c70"

    when:
      jiraNotifier.send(true,
                        new JiraNotification(true,
                                             false,
                                             'projectKey',
                                             null,
                                             null,
                                             null,
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             false,
                                             false,
                                             "overrideId",
                                             "overrideId",
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null),
                        pehaMock)

    then:
      1 * IQClientFactory.getIQClient('overrideId', *_ ) >> iqClient
      1 * JiraClientFactory.getJiraClient('overrideId', *_) >> jiraClient
  }

  def 'send expands notification arguments'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

      PolicyEvaluationHealthAction pehaMock  = Mock(PolicyEvaluationHealthAction)
      pehaMock.getReportLink() >> "http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/6aeee85d9f8d45abbd91859352742c70"

    when:
      mockRun.getEnvironment(_) >> ['projectKey': 'project']
      jiraNotifier.send(true,
                        new JiraNotification(true,
                                             false,
                                             '${projectKey}',
                                             null,
                                             null,
                                             null,
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             false,
                                             false,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null),
                        pehaMock)

    then:
      1 * jiraClient.createIssue(*_) >> { arguments ->
        def jiraFieldMappingUtil = arguments[0] as JiraFieldMappingUtil
        assert jiraFieldMappingUtil.projectKey == "project"
      }
  }

  @Ignore //todo: read in JSON for Jira & IQ Mocks???
  def 'putsCard to Jira client'() {
    setup:
      def policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
          reportLink: 'http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/6aeee85d9f8d45abbd91859352742c70',
          affectedComponentCount: 1,
          criticalComponentCount: 2,
          severeComponentCount: 3,
          moderateComponentCount: 5
        )

      def jiraNotification = new JiraNotification(true,
                                                  true,
                                                  'JIRAIQ',
                                                  "Task",
                                                  "Sub-task",
                                                  "Low",
                                                  false,
                                                  true,
                                                  "Done",
                                                  "IQ Application",
                                                  "IQ Organization",
                                                  null,
                                                  "Finding ID",
                                                  "Detect Date",
                                                  "Last Scan Date",
                                                  null,
                                                  null,
                                                  null,
                                                  "Security-High",
                                                  false,
                                                  false,
                                                  null,
                                                  null,
                                                  "Scan Type",
                                                  "SCA",
                                                  "Tool Name",
                                                  "Nexus IQ",
                                                  "Finding Template",
                                                  "NA")

      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient


    when:
      jiraNotifier.send(buildPassing, jiraNotification, policyEvaluationHealthAction)

    then:
      1 * client.lookupMetadataConfigurationForCreateIssue(_) >> { arugments ->
        def projectKey = arugments[0] as String
        assert policyEvaluationResult == 'JIRAIQ'

        def issueTypeName = arugments[1] as String
        assert issueTypeName == 'Task'
      }

      1 * client.lookupCustomFields() >> new HashMap()


//              { arugments ->
//        def policyEvaluationResult = arugments[0] as PolicyEvaluationResult
//        assert policyEvaluationResult.projectKey == jiraNotification.projectKey
//        assert policyEvaluationResult.repositorySlug == jiraNotification.repositorySlug
//        assert policyEvaluationResult.commitHash == jiraNotification.commitHash
//        assert policyEvaluationResult.buildStatus == (buildPassing ? BuildStatus.PASS : BuildStatus.FAIL)
//        assert policyEvaluationResult.componentsAffected == policyEvaluationHealthAction.affectedComponentCount
//        assert policyEvaluationResult.critical == policyEvaluationHealthAction.criticalComponentCount
//        assert policyEvaluationResult.severe == policyEvaluationHealthAction.severeComponentCount
//        assert policyEvaluationResult.moderate == policyEvaluationHealthAction.moderateComponentCount
//        assert policyEvaluationResult.reportUrl == policyEvaluationHealthAction.reportLink
//      }

    where:
      //buildPassing << [true, false ]
      buildPassing << true
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Create Summary Ticket'() {
    setup:
    def jiraClient = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', System.out, true)
    def iqClient = new IQClient("http://localhost:${iqPort}/iq", 'admin', 'admin123', System.out, true)

    def policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
            reportLink: 'http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/6aeee85d9f8d45abbd91859352742c70',
            affectedComponentCount: 1,
            criticalComponentCount: 2,
            severeComponentCount: 3,
            moderateComponentCount: 5
    )

    def jiraNotification = new JiraNotification(true,
                                                true,
                                                'JIRAIQ',
                                                "Bug",
                                                "Sub-task",
                                                "Low",
                                                false,
                                                true,
                                                "Done",
                                                "IQ Application",
                                                "IQ Organization",
                                                null,
                                                "Finding ID",
                                                "Detect Date",
                                                "Last Scan Date",
                                                null,
                                                null,
                                                null,
                                                "Security-High",
                                                false,
                                                false,
                                                null,
                                                null,
                                                "Scan Type",
                                                "SCA",
                                                "Tool Name",
                                                "Nexus IQ",
                                                "Finding Template",
                                                "NA")

    GroovyMock(JiraClientFactory.class, global: true)
    JiraClientFactory.getJiraClient(*_) >> jiraClient

    GroovyMock(IQClientFactory.class, global: true)
    IQClientFactory.getIQClient(*_) >> iqClient

    jiraNotifier.send(true, jiraNotification, policyEvaluationHealthAction)

    expect:
    true //TODO: assert something??
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets'() {
    setup:
    boolean verboseLogging = false

    def jiraClient = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', System.out, verboseLogging)
    def iqClient = new IQClient("http://localhost:${iqPort}/iq", 'admin', 'admin123', System.out, verboseLogging)

    def policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
            reportLink: 'http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/6aeee85d9f8d45abbd91859352742c70',
            affectedComponentCount: 1,
            criticalComponentCount: 2,
            severeComponentCount: 3,
            moderateComponentCount: 5
    )

    def jiraNotification = new JiraNotification(true,
                                                verboseLogging,
                                                'JIRAIQ',
                                                "Bug",
                                                "Sub-task",
                                                "Low",
                                                true,
                                                true,
                                                "Done",
                                                "IQ Application",
                                                "IQ Organization",
                                                null,
                                                "Finding ID",
                                                "Detect Date",
                                                "Last Scan Date",
                                                null,
                                                null,
                                                null,
                                                "Security-High",
//                                                "License",
                                                false,
                                                false,
                                                null,
                                                null,
                                                "Scan Type",
                                                "SCA",
                                                "Tool Name",
                                                "Nexus IQ",
                                                "Finding Template",
                                                "NA")

    GroovyMock(JiraClientFactory.class, global: true)
    JiraClientFactory.getJiraClient(*_) >> jiraClient

    GroovyMock(IQClientFactory.class, global: true)
    IQClientFactory.getIQClient(*_) >> iqClient

    jiraNotifier.send(true, jiraNotification, policyEvaluationHealthAction)

    expect:
    true //TODO: assert something??
  }

}
