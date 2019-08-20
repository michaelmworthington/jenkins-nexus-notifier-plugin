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

import groovy.json.JsonSlurper
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.iq.IQClient
import org.sonatype.nexus.ci.jenkins.iq.IQClientFactory
import org.sonatype.nexus.ci.jenkins.model.PolicyEvaluationHealthAction
import org.sonatype.nexus.ci.jenkins.notifier.ContinuousMonitoringConfig
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import org.sonatype.nexus.ci.jenkins.util.JiraFieldMappingUtil
import spock.lang.Requires
import spock.lang.Specification

//TODO: Think about removing these or making the JiraNotification sharing better
@SuppressWarnings("GroovyAccessibility")
class JiraNotifierTest
    extends Specification
{
  //private static final String jiraPort = "59454" //for Charles Proxy
  private static final String jiraPort = "8080"
  //private static final String iqPort = "60359" //for Charles Proxy
  private static final String iqPort = "8060"

  private static final String iqTestReport = "e8ef4d3d26dd48b3866019b1478c6453" //demo (69)
  //private static final String iqTestReport = "73cbe478f71a4b8382d773a4248f2f88" //test (59)

  private static final String iqTestAppExternalId = "aaaaaaa-testidegrandfathering"
  private static final String iqTestAppInternalId = "e06a119c75d04d97b8d8c11b62719752"

  IQClient iqClient, integrationTestIqClient
  JiraClient jiraClient, integrationTestJiraClient
  def jqlMaxResultsOverride = 50
  def disableJqlFieldFilter = false
  def disableIQCVEDetails = false
  def disableIQRemediationRecommendation = false
  def dryRun = false

  boolean verboseLogging = true
  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  def iqApplication, iqApplicationEmpty, customFields, iqReport, iqReportRaw
  String dynamicDataTwo, dynamicDataOne

  PolicyEvaluationHealthAction policyEvaluationHealthAction
  JiraNotification jiraNotificationCreateParentTicketTest, jiraNotificationMinimalTest
  ContinuousMonitoringConfig continuousMonitoringConfig = new ContinuousMonitoringConfig()

  JiraNotifier jiraNotifier

  def setup() {
    GroovyMock(JiraClientFactory.class, global: true)
    jiraClient = Mock(JiraClient.class)

    GroovyMock(IQClientFactory.class, global: true)
    iqClient = Mock(IQClient.class)

    integrationTestJiraClient = Spy(JiraClient, constructorArgs: ["http://localhost:${jiraPort}", 'admin', 'admin123', mockLogger, verboseLogging, dryRun, disableJqlFieldFilter, jqlMaxResultsOverride])
    integrationTestIqClient = Spy(IQClient, constructorArgs: ["http://localhost:${iqPort}/iq", 'admin', 'admin123', mockLogger, verboseLogging, disableIQCVEDetails, disableIQRemediationRecommendation])

    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]
    jiraNotifier = new JiraNotifier(mockRun, mockListener)

    iqApplication = new JsonSlurper().parse(new File("src/test/resources/iq-${iqTestAppExternalId}-applicationInfo.json"))
    iqApplicationEmpty = new JsonSlurper().parse(new File("src/test/resources/iq-applications-empty.json"))
    customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))
    iqReport = new JsonSlurper().parse(new File("src/test/resources/iq-${iqTestAppExternalId}-${iqTestReport}-policythreats.json"))
    iqReportRaw = new JsonSlurper().parse(new File("src/test/resources/iq-report-raw-${iqTestAppExternalId}-${iqTestReport}.json"))

    policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
            reportLink: "http://localhost:${iqPort}/iq/ui/links/application/${iqTestAppExternalId}/report/${iqTestReport}",
            affectedComponentCount: 1,
            criticalComponentCount: 2,
            severeComponentCount: 3,
            moderateComponentCount: 5
    )

    continuousMonitoringConfig.shouldRunWithContinuousMonitoring = true
    continuousMonitoringConfig.dynamicDataStageKey = "stage"
    continuousMonitoringConfig.dynamicDataApplicationKey = "iqLookup"
    continuousMonitoringConfig.shouldUpdateLastScanDate = true

    dynamicDataTwo = new File("src/test/resources/continuous-monitor-dynamic-data-two.json").text
    dynamicDataOne = new File("src/test/resources/continuous-monitor-dynamic-data-one.json").text

    jiraNotificationMinimalTest = new JiraNotification(true,
                                                       'JIRAIQ',
                                                       "Bug",
                                                       "Sub-task",
                                                       "Low",
                                                       true,
                                                       false,
                                                       false,
                                                       true,
                                                       "Done",
                                                       "Resolve",
                                                       null,
                                                       -1,
                                                       null,
                                                       null,
                                                       verboseLogging,
                                                       dryRun,
                                                       disableIQCVEDetails,
                                                       disableIQRemediationRecommendation,
                                                       null,
                                                       disableJqlFieldFilter,
                                                       jqlMaxResultsOverride,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       "Finding ID",
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
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null,
                                                       null)

    jiraNotificationCreateParentTicketTest = new JiraNotification(true,
                                                                  'JIRAIQ',
                                                                  "Bug",
                                                                  "Sub-task",
                                                                  "Low",
                                                                  true,
                                                                  false,
                                                                  false,
                                                                  true,
                                                                  "Done",
                                                                  "Resolve",
                                                                  "License",
                                                                  0,
                                                                  null,
                                                                  null,
                                                                  verboseLogging,
                                                                  dryRun,
                                                                  disableIQCVEDetails,
                                                                  disableIQRemediationRecommendation,
                                                                  null,
                                                                  disableJqlFieldFilter,
                                                                  jqlMaxResultsOverride,
                                                                  null,
                                                                  "IQ Application",
                                                                  "IQ Organization",
                                                                  "Scan Stage",
                                                                  "Finding ID",
                                                                  "Detect Date",
                                                                  "Last Scan Date",
                                                                  "Severity",
                                                                  "Max Severity",
                                                                  "CVE Code",
                                                                  "Max CVE Code",
                                                                  "CVE Link",
                                                                  "CVE Link",
                                                                  "CWE Code",
                                                                  "CWE Code",
                                                                  "Threat Vector",
                                                                  "Threat Vector",
                                                                  "CVSS",
                                                                  "Max CVSS",
                                                                  "Report Link",
                                                                  "Violation Name",
                                                                  "Max Violation Name",
                                                                  "Threat Level",
                                                                  "Max Threat Level",
                                                                  "Declared License",
                                                                  "Observed License",
                                                                  "Finding License",
                                                                  "Finding File Occurrences",
                                                                  "Recommended Version",
                                                                  "PURL",
                                                                  "Finding Component Identifier",
                                                                  "Finding Vendor",
                                                                  "Finding Library",
                                                                  "Finding Version",
                                                                  "Finding Classifier",
                                                                  "Finding Extension",
                                                                  [
                                                                          [ customFieldName: 'Random Number', customFieldValue: '17'],
                                                                          [ customFieldName: 'Scan Type', customFieldValue: 'SCA'],
                                                                          [ customFieldName: 'Finding Template', customFieldValue: 'NA'],
                                                                          [ customFieldName: 'Tool Name', customFieldValue: 'Nexus IQ'],
                                                                          [ customFieldName: 'Scan Stage', customFieldValue: 'Build'],
                                                                          [ customFieldName: 'App Mnemonic', dynamicDataCustomFieldValue: "appName"],
                                                                          [ customFieldName: 'App Mnemonic IDIP', dynamicDataCustomFieldValue: "mnemonic"],
                                                                          [ customFieldName: 'Copied Detect Date', copyValueFromFieldName: "Detect Date"],
                                                                          [ customFieldName: 'Copied Finding License', copyValueFromFieldName: "Finding License"],
                                                                  ])
  }

  def 'send requires projectKey'() {
    setup:
      jiraNotificationCreateParentTicketTest.projectKey = emptyOptions

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest,null)

    then:
      IllegalArgumentException ex = thrown()
      ex.message == 'Jira Project Key is a required argument for the Jira Notifier'

    where:
      emptyOptions << [ null, '' ]
  }

  def 'creates Jira client with job credentials override'() {
    setup:
      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotificationCreateParentTicketTest.projectKey = 'projectKey'
      jiraNotificationCreateParentTicketTest.jobJiraCredentialsId = "overrideId"
      jiraNotificationCreateParentTicketTest.jobIQCredentialsId = "overrideId"
      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * IQClientFactory.getIQClient('overrideId', *_ ) >> iqClient
      1 * JiraClientFactory.getJiraClient('overrideId', *_) >> jiraClient
      1 * jiraClient.lookupCustomFields() >> customFields

  }

  def 'send expands notification arguments'() {
    setup:
      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotificationCreateParentTicketTest.projectKey = '${projectKey}'
      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      mockRun.getEnvironment(_) >> ['projectKey': 'project']
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * jiraClient.createIssue(*_) >> { arguments ->
        def jiraFieldMappingUtil = arguments[0] as JiraFieldMappingUtil
        assert jiraFieldMappingUtil.projectKey == "project"
      }
  }

  def 'Create Summary Ticket'() {
    setup:
      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields

      //Expectations
      1 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - No Aggregation - No Jira Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set.json'))

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security-High"

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      2 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - No Aggregation - Update Existing License Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-license-tickets-no-aggregation.json'))

      jiraNotificationCreateParentTicketTest.lastScanDateCustomFieldName = lastScanDateFieldName

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      updateCount * jiraClient.updateIssueScanDate(*_)

    where:
      lastScanDateFieldName | updateCount
      'Last Scan Date'      | 2
      null                  | 0
  }

  def 'Create Detail Tickets - No Aggregation - Close Old Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-no-aggregation.json'))

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      2 * jiraClient.createIssue(*_)
      11 * jiraClient.closeTicket(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - No Jira Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - Update Existing License Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-license-tickets-aggregated-no-subtasks.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - Close Old Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-no-subtasks.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_)
      2 * jiraClient.closeTicket(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - No Jira Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set.json'))

      def createParentTicket = new JsonSlurper().parse(new File('src/test/resources/jira-create-parent-ticket-response.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_) >> createParentTicket
      2 * jiraClient.createSubTask(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - Update Existing License Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-license-tickets-aggregated-and-subtasks.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      3 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - Update Existing Security High Tickets and add other Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks.json'))

      jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security"
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      9 * iqClient.lookupCweAndThreatVector(_) >> ["",""]
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      9 * jiraClient.createSubTask(*_)
      12 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - Update Existing Security High Tickets - Jira Ticket Query Paging Test - 113 results'()
  {
    setup:
      def openTickets0 = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks-from-jenkinsfile-test-0-49.json'))
      def openTickets50 = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks-from-jenkinsfile-test-50-99.json'))
      def openTickets100 = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks-from-jenkinsfile-test-100-113.json'))

      def iqReportBig = new JsonSlurper().parse(new File("src/test/resources/iq-${iqTestAppExternalId}-67b95f188e3f4a9896a370d7bc830cc8-policythreats.json"))
      policyEvaluationHealthAction.reportLink = "http://localhost:${iqPort}/iq/ui/links/application/${iqTestAppExternalId}/report/67b95f188e3f4a9896a370d7bc830cc8"

      jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security-High"
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("67b95f188e3f4a9896a370d7bc830cc8", iqTestAppExternalId) >> iqReportBig
      1 * iqClient.lookupComponentDetailsFromIQ("67b95f188e3f4a9896a370d7bc830cc8", iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, 0) >> openTickets0
      1 * jiraClient.lookupJiraTickets(_, 50) >> openTickets50
      1 * jiraClient.lookupJiraTickets(_, 100) >> openTickets100

      //Expectations when doing License Policy Filter
      0 * jiraClient.createIssue(*_)
      0 * jiraClient.createSubTask(*_)
      113 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - JQL Override of Zero throws an Exception'()
  {
    setup:
      def openTicketsBad = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set-bad-maxResults.json'))

      def iqReportBig = new JsonSlurper().parse(new File("src/test/resources/iq-${iqTestAppExternalId}-67b95f188e3f4a9896a370d7bc830cc8-policythreats.json"))
      policyEvaluationHealthAction.reportLink = "http://localhost:${iqPort}/iq/ui/links/application/${iqTestAppExternalId}/report/67b95f188e3f4a9896a370d7bc830cc8"

      jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security-High"
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("67b95f188e3f4a9896a370d7bc830cc8", iqTestAppExternalId) >> iqReportBig
      1 * iqClient.lookupComponentDetailsFromIQ("67b95f188e3f4a9896a370d7bc830cc8", iqTestAppExternalId) >> iqReportRaw
      jiraClient.lookupJiraTickets(_, _) >> openTicketsBad

      def e = thrown(hudson.AbortException)
      e.message == "Invalid Configuration: Search start and finish are the same."

  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - Close Old Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks.json'))

      def createParentTicket = new JsonSlurper().parse(new File('src/test/resources/jira-create-parent-ticket-response.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      JiraClientFactory.getJiraClient(*_) >> jiraClient
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
      1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_) >> createParentTicket
      2 * jiraClient.createSubTask(*_)
      12 * jiraClient.closeTicket(*_)
  }

  def 'Test Dynamic Data'() {
    setup:
      String dynamicData = '[ {"mnemonic": "aaaaaaa", "appName": "testidegrandfathering", "iqLookup": "aaaaaaa-testidegrandfathering" }, \
                              {"mnemonic": "04", "appName": "explicit-staging-maven-plugin", "iqLookup": "04-explicit-staging-maven-plugin" } ]'

    when:
      def o = new JsonSlurper().parseText(dynamicData)

    then:
      o.size == 2
      String fieldName = "appName"

      o.each{
        it[fieldName] != null
      }
  }

  def 'Continuous Monitoring Create Detail Tickets - Aggregate by Component and SubTask - Update Existing Security High Tickets and add other Security Tickets'() {
    setup:
    def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks.json'))

    jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security"
    jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
    jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

    JiraClientFactory.getJiraClient(*_) >> jiraClient
    IQClientFactory.getIQClient(*_) >> iqClient

    when:
    jiraNotifier.continuousMonitor(dynamicDataOne, continuousMonitoringConfig, jiraNotificationCreateParentTicketTest)

    then:
    2 * iqClient.lookupApplication(iqTestAppExternalId) >> iqApplication
    1 * iqClient.lookupReportLinkForInternalId(iqTestAppInternalId, "stage-release") >> "http://localhost:${iqPort}/iq/ui/links/application/${iqTestAppExternalId}/report/${iqTestReport}"
    1 * jiraClient.lookupCustomFields() >> customFields
    1 * iqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReport
    1 * iqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId) >> iqReportRaw
    9 * iqClient.lookupCweAndThreatVector(_) >> ["",""]
    1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

    //Expectations when doing License Policy Filter
    9 * jiraClient.createSubTask(*_)
    12 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Continuous Monitoring Create Detail Tickets - Aggregate by Component and SubTask - Invalid Application ID throws Exception'() {
    setup:
    jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security"
    jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
    jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

    JiraClientFactory.getJiraClient(*_) >> jiraClient
    IQClientFactory.getIQClient(*_) >> iqClient

    when:
    jiraNotifier.continuousMonitor(dynamicDataOne, continuousMonitoringConfig, jiraNotificationCreateParentTicketTest)

    then:
    1 * iqClient.lookupApplication(iqTestAppExternalId) >> iqApplicationEmpty
    IllegalArgumentException ex = thrown()
    ex.message == 'Continuous Monitoring Application was not found: aaaaaaa-testidegrandfathering'
  }

  def 'Continuous Monitoring Create Detail Tickets - Aggregate by Component and SubTask - Application has no report at stage'() {
    setup:
    jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security"
    jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
    jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

    JiraClientFactory.getJiraClient(*_) >> jiraClient
    IQClientFactory.getIQClient(*_) >> iqClient

    when:
    continuousMonitoringConfig.dynamicDataStageKey = null
    continuousMonitoringConfig.stage = "operate"

    jiraNotifier.continuousMonitor(dynamicDataOne, continuousMonitoringConfig, jiraNotificationCreateParentTicketTest)

    then:
    1 * iqClient.lookupApplication(iqTestAppExternalId) >> iqApplication
    1 * iqClient.lookupReportLinkForInternalId(iqTestAppInternalId, "operate") >> null
    0 * jiraClient.lookupCustomFields()
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Summary Ticket'() {
    setup:
      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

      JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
      IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      true
      1 * integrationTestJiraClient.lookupCustomFields()

      1 * integrationTestJiraClient.createIssue(*_)

    cleanup:
      System.out.println("close the 2 tickets") //todo

  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - No Aggregation'() {
    setup:
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = false
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = false

      JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
      IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * integrationTestJiraClient.lookupCustomFields()
      1 * integrationTestIqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestIqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestJiraClient.lookupJiraTickets(_, _)

      //Expectations when doing License Policy Filter AND no existing Jira Tickets
      2 * integrationTestJiraClient.createIssue(*_)
      0 * integrationTestJiraClient.updateIssueScanDate(*_)
      //TODO: when run in sequence, it'll close the summary ticket from above
      1 * integrationTestJiraClient.closeTicket(*_)

    when: 'Close all the tickets'
      jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      2 * integrationTestJiraClient.closeTicket(*_)
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - Aggregate by Component No SubTasks'() {
    setup:
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = false

      JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
      IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      true
      1 * integrationTestJiraClient.lookupCustomFields()
      1 * integrationTestIqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestIqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestJiraClient.lookupJiraTickets(_, _)

      //Expectations when doing License Policy Filter AND no existing Jira Tickets
      1 * integrationTestJiraClient.createIssue(*_)
      0 * integrationTestJiraClient.updateIssueScanDate(*_)

    when: 'Close all the tickets'
      jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * integrationTestJiraClient.closeTicket(*_)
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - Aggregate by Component and SubTasks'() {
    setup:
      jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'License-Non'
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = null
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true
      //skip updating the last scan date to speed up things
      //jiraNotificationCreateParentTicketTest.lastScanDateCustomFieldName = null

      // The report from Juice Shop and Goof with lots of results
      //policyEvaluationHealthAction.reportLink = 'http://localhost:${iqPort}/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/df53830759574e71a645d40839dc531f'

      //need these so we're not calling back to the jenkins runtime
      JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
      IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
      jiraNotifier.send(true, dynamicDataOne, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * integrationTestJiraClient.lookupCustomFields()
      1 * integrationTestIqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestIqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId)
      1 * integrationTestJiraClient.lookupJiraTickets(_, _)

      //Expectations when doing Security-High Policy Filter
      2 * integrationTestJiraClient.createIssue(*_)
      10 * integrationTestJiraClient.createSubTask(*_)

    when:
      jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
      jiraNotifier.send(true, null, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      12 * integrationTestJiraClient.closeTicket(*_)
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Continuous Monitoring Create Detail Tickets - Aggregate by Component and SubTasks'() {
    setup:
    jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
    //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'License-Non'
    //jiraNotificationCreateParentTicketTest.policyFilterPrefix = null
    jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
    jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true
    //skip updating the last scan date to speed up things
    //jiraNotificationCreateParentTicketTest.lastScanDateCustomFieldName = null

    // The report from Juice Shop and Goof with lots of results
    //policyEvaluationHealthAction.reportLink = 'http://localhost:${iqPort}/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/df53830759574e71a645d40839dc531f'

    //need these so we're not calling back to the jenkins runtime
    JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
    IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
    jiraNotifier.continuousMonitor(dynamicDataTwo, continuousMonitoringConfig, jiraNotificationCreateParentTicketTest)

    then:
    2 * integrationTestJiraClient.lookupCustomFields()
    1 * integrationTestIqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId)
    1 * integrationTestIqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId)
    1 * integrationTestIqClient.lookupPolcyDetailsFromIQ("f4dd9636ec724763a38ac0000bd7e0e7", "04-explicit-staging-nexus2-plugin")
    1 * integrationTestIqClient.lookupComponentDetailsFromIQ("f4dd9636ec724763a38ac0000bd7e0e7", "04-explicit-staging-nexus2-plugin")
    2 * integrationTestJiraClient.lookupJiraTickets(_, _)

    //Expectations when doing License Policy Filter
    9 * integrationTestJiraClient.createIssue(*_)
    20 * integrationTestJiraClient.createSubTask(*_)

    when:
    jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
    jiraNotifier.continuousMonitor(dynamicDataTwo, continuousMonitoringConfig, jiraNotificationCreateParentTicketTest)

    then:
    29 * integrationTestJiraClient.closeTicket(*_)
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - Aggregate by Component and SubTasks - Minimal Config Test'() {
    setup:
    jiraNotificationMinimalTest.policyFilterPrefix = ''
    jiraNotificationMinimalTest.policyFilterThreatLevel = 0
    jiraNotificationMinimalTest.shouldAggregateTicketsByComponent = true
    jiraNotificationMinimalTest.shouldCreateSubTasksForAggregatedTickets = true

    //need these so we're not calling back to the jenkins runtime
    JiraClientFactory.getJiraClient(*_) >> integrationTestJiraClient
    IQClientFactory.getIQClient(*_) >> integrationTestIqClient

    when:
    jiraNotifier.send(true, dynamicDataOne, jiraNotificationMinimalTest, policyEvaluationHealthAction)

    then:
    1 * integrationTestJiraClient.lookupCustomFields()
    1 * integrationTestIqClient.lookupPolcyDetailsFromIQ(iqTestReport, iqTestAppExternalId)
    1 * integrationTestIqClient.lookupComponentDetailsFromIQ(iqTestReport, iqTestAppExternalId)
    1 * integrationTestJiraClient.lookupJiraTickets(_, _)

    when:
    jiraNotificationMinimalTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
    jiraNotifier.send(true, null, jiraNotificationMinimalTest, policyEvaluationHealthAction)

    then:
    true
  }

}
