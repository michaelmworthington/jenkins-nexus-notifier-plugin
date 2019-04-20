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

  boolean verboseLogging = true
  //def mockLogger = Mock(PrintStream)
  def mockLogger = System.out
  def mockListener = Mock(TaskListener)
  def mockRun = Mock(Run)

  def customFields, iqReport, iqApplication, iqOrganization

  PolicyEvaluationHealthAction policyEvaluationHealthAction
  JiraNotification jiraNotificationCreateParentTicketTest

  JiraNotifier jiraNotifier

  def setup() {
    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]
    jiraNotifier = new JiraNotifier(mockRun, mockListener)

    customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))
    iqReport = new JsonSlurper().parse(new File('src/test/resources/iq-aaaaaaa-testidegrandfathering-e8ef4d3d26dd48b3866019b1478c6453-policythreats.json'))
    iqApplication = new JsonSlurper().parse(new File('src/test/resources/iq-aaaaaaa-testidegrandfathering-applicationInfo.json'))
    iqOrganization = new JsonSlurper().parse(new File('src/test/resources/iq-organizations.json'))

    policyEvaluationHealthAction = new PolicyEvaluationHealthAction(
            reportLink: 'http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/e8ef4d3d26dd48b3866019b1478c6453',
            affectedComponentCount: 1,
            criticalComponentCount: 2,
            severeComponentCount: 3,
            moderateComponentCount: 5
    )

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
                                                                  "License",
                                                                  null,
                                                                  null,
                                                                  verboseLogging,
                                                                  false,
                                                                  null,
                                                                  false,
                                                                  5,
                                                                  null,
                                                                  "IQ Application",
                                                                  "IQ Organization",
                                                                  "Scan Stage",
                                                                  "Finding ID",
                                                                  "Detect Date",
                                                                  "Last Scan Date",
                                                                  "Severity",
                                                                  "CVE Code",
                                                                  "CVSS",
                                                                  "Report Link",
                                                                  "Violation Name",
                                                                  "Threat Level",
                                                                  [
                                                                          [ customFieldName: 'Random Number', customFieldValue: '17'],
                                                                          [ customFieldName: 'Scan Type', customFieldValue: 'SCA'],
                                                                          [ customFieldName: 'Finding Template', customFieldValue: 'NA'],
                                                                          [ customFieldName: 'Tool Name', customFieldValue: 'Nexus IQ'],
                                                                          [ customFieldName: 'Scan Stage', customFieldValue: 'Build']
                                                                  ])
  }

  def 'send requires projectKey'() {
    setup:
      jiraNotificationCreateParentTicketTest.projectKey = emptyOptions

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest,null)

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

      jiraNotificationCreateParentTicketTest.projectKey = 'projectKey'
      jiraNotificationCreateParentTicketTest.jobJiraCredentialsId = "overrideId"
      jiraNotificationCreateParentTicketTest.jobIQCredentialsId = "overrideId"
      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * IQClientFactory.getIQClient('overrideId', *_ ) >> iqClient
      1 * JiraClientFactory.getJiraClient('overrideId', *_) >> jiraClient
      1 * jiraClient.lookupCustomFields() >> customFields

  }

  def 'send expands notification arguments'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotificationCreateParentTicketTest.projectKey = '${projectKey}'
      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      mockRun.getEnvironment(_) >> ['projectKey': 'project']
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * jiraClient.createIssue(*_) >> { arguments ->
        def jiraFieldMappingUtil = arguments[0] as JiraFieldMappingUtil
        assert jiraFieldMappingUtil.projectKey == "project"
      }
  }

  def 'Create Summary Ticket'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields

      //Expectations
      1 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - No Aggregation - No Jira Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set.json'))

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = "Security-High"

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      2 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - No Aggregation - Update Existing License Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-license-tickets-no-aggregation.json'))

      jiraNotificationCreateParentTicketTest.lastScanDateCustomFieldName = lastScanDateFieldName

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
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

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      //Load JSON Data
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      2 * jiraClient.createIssue(*_)
      11 * jiraClient.closeTicket(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - No Jira Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-tickets-empty-set.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - Update Existing License Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-license-tickets-aggregated-no-subtasks.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component No SubTasks - Close Old Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-no-subtasks.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
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

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
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

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
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

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      9 * jiraClient.createSubTask(*_)
      12 * jiraClient.updateIssueScanDate(*_)
  }

  def 'Create Detail Tickets - Aggregate by Component and SubTask - Close Old Security Tickets'() {
    setup:
      def openTickets = new JsonSlurper().parse(new File('src/test/resources/jira-open-security-tickets-aggregated-and-subtasks.json'))

      def createParentTicket = new JsonSlurper().parse(new File('src/test/resources/jira-create-parent-ticket-response.json'))

      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true

      GroovyMock(JiraClientFactory.class, global: true)
      def jiraClient = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      def iqClient = Mock(IQClient.class)
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      1 * jiraClient.lookupCustomFields() >> customFields
      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering") >> iqReport
      1 * jiraClient.lookupJiraTickets(_, _) >> openTickets

      //Expectations when doing License Policy Filter
      1 * jiraClient.createIssue(*_) >> createParentTicket
      2 * jiraClient.createSubTask(*_)
      12 * jiraClient.closeTicket(*_)
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Summary Ticket'() {
    setup:
      def jiraClient = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', mockLogger, verboseLogging)
      def iqClient = new IQClient("http://localhost:${iqPort}/iq", 'admin', 'admin123', mockLogger, verboseLogging)

      jiraNotificationCreateParentTicketTest.shouldCreateIndividualTickets = false

      GroovyMock(JiraClientFactory.class, global: true)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    expect:
      true //TODO: assert something??
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - No Aggregation'() {
    setup:
      def jiraClient = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', mockLogger, verboseLogging)
      def iqClient = new IQClient("http://localhost:${iqPort}/iq", 'admin', 'admin123', mockLogger, verboseLogging)

      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = false
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = false

      GroovyMock(JiraClientFactory.class, global: true)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    expect:
      true //TODO: assert something??
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - Aggregate by Component No SubTasks'() {
    setup:
      def jiraClient = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', mockLogger, verboseLogging)
      def iqClient = new IQClient("http://localhost:${iqPort}/iq", 'admin', 'admin123', mockLogger, verboseLogging)

      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = false

      GroovyMock(JiraClientFactory.class, global: true)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true)
      IQClientFactory.getIQClient(*_) >> iqClient

      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    expect:
      true //TODO: assert something??
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with Jira Server - Create Detail Tickets - Aggregate by Component and SubTasks'() {
    setup:
      def jqlMaxResultsOverride = 2
      def disableJqlFieldFilter = true
      def dryRun = true

      def jiraClient = Spy(JiraClient, constructorArgs: ["http://localhost:${jiraPort}", 'admin', 'admin123', mockLogger, verboseLogging, dryRun, disableJqlFieldFilter, jqlMaxResultsOverride]) //TODO: make these class level declarations
      def iqClient = Spy(IQClient, constructorArgs: ["http://localhost:${iqPort}/iq", 'admin', 'admin123', mockLogger, verboseLogging])

      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'Security-High'
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'License-Non'
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = null
      jiraNotificationCreateParentTicketTest.shouldAggregateTicketsByComponent = true
      jiraNotificationCreateParentTicketTest.shouldCreateSubTasksForAggregatedTickets = true
      //skip updating the last scan date to speed up things
      jiraNotificationCreateParentTicketTest.lastScanDateCustomFieldName = null

      // The report from Juice Shop and Goof with lots of results
      //policyEvaluationHealthAction.reportLink = 'http://localhost:8060/iq/ui/links/application/aaaaaaa-testidegrandfathering/report/df53830759574e71a645d40839dc531f'

      //need these so we're not calling back to the jenkins runtime
      GroovyMock(JiraClientFactory.class, global: true)
      JiraClientFactory.getJiraClient(*_) >> jiraClient

      GroovyMock(IQClientFactory.class, global: true) //TODO: make these class level declarations
      IQClientFactory.getIQClient(*_) >> iqClient

    when:
      jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      true
//      1 * jiraClient.lookupCustomFields()
//      1 * iqClient.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering")
//      1 * jiraClient.lookupJiraTickets(_, _)
//
//      //Expectations when doing License Policy Filter
//      1 * jiraClient.createIssue(*_)
//      1 * jiraClient.createSubTask(*_)

    when:
      //jiraNotificationCreateParentTicketTest.policyFilterPrefix = 'A Fake License So All The Tickets Get Closed'
      //jiraNotifier.send(true, jiraNotificationCreateParentTicketTest, policyEvaluationHealthAction)

    then:
      true
    //3 * jiraClient.closeTicket(*_)
  }
}
