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
package org.sonatype.nexus.ci.jenkins.util

import groovy.json.JsonSlurper
import hudson.EnvVars
import hudson.model.Run
import hudson.model.TaskListener
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import org.sonatype.nexus.ci.jenkins.jira.JiraClientFactory
import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification
import spock.lang.Ignore
import spock.lang.Specification

class JiraFieldMappingUtilTest
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

  JiraNotification jiraNotificationCustomFieldMapTest

  def setup() {
    mockListener.getLogger() >> mockLogger
    mockRun.getEnvironment(_) >> [:]

    jiraNotificationCustomFieldMapTest = new JiraNotification(true,
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
  }

  def 'expands arguments'() {
    setup:
      EnvVars ev = ['projectKey': 'project']
      jiraNotificationCustomFieldMapTest.projectKey = '${projectKey}'

      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, null, ev, mockLogger)

    when:
      jiraFieldMappingUtil.expandEnvVars()

    then:
      assert jiraFieldMappingUtil.projectKey == "project"
  }

  def 'validate custom fields names are mapped to ids - app name'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, client, mockRun.getEnvironment(mockListener), mockLogger)
      jiraFieldMappingUtil.applicationCustomFieldName = jiraNotificationCustomFieldMapTest.applicationCustomFieldName

      List<Map<String, Object>> customFields = [
                  [
                          "id": "customfield_10200",
                          "name": "IQ Application"
                  ]
          ]

    when:
      //jiraFieldMappingUtil.expandEnvVars()
      //jiraFieldMappingUtil.assignFieldsFromConfig()
      jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    then:
      1 * client.lookupCustomFields() >> customFields

      assert jiraFieldMappingUtil.applicationCustomFieldId == "customfield_10200"
  }

  def 'validate custom fields names are mapped to ids - all fields from jsonslurper'() {
    setup:
      GroovyMock(JiraClientFactory.class, global: true)
      def client = Mock(JiraClient.class)
      JiraClientFactory.getJiraClient(*_) >> client

      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, client, mockRun.getEnvironment(mockListener), mockLogger)

      def customFields = new JsonSlurper().parse(new File('src/test/resources/jira-custom-fields.json'))

    when:
      jiraFieldMappingUtil.expandEnvVars()
      jiraFieldMappingUtil.assignFieldsFromConfig()
      jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    then:
      1 * client.lookupCustomFields() >> customFields

      assert jiraFieldMappingUtil.applicationCustomFieldId == "customfield_10200"
      assert jiraFieldMappingUtil.organizationCustomFieldId == "customfield_10201"
      assert jiraFieldMappingUtil.violationIdCustomFieldId == "customfield_10300"
      assert jiraFieldMappingUtil.violationDetectDateCustomFieldId == "customfield_10502"
      assert jiraFieldMappingUtil.lastScanDateCustomFieldId == "customfield_1010503"
      assert jiraFieldMappingUtil.scanTypeCustomFieldId == "customfield_10400"
      assert jiraFieldMappingUtil.toolNameCustomFieldId == "customfield_10501"
      assert jiraFieldMappingUtil.findingTemplateCustomFieldId == "customfield_10500"
      //TODO: update with the rest of the fields
  }

  @Ignore
  def 'helper test to verify interaction with Jira Server - Map Custom Fields'() {
    setup:
      def client = new JiraClient("http://localhost:${jiraPort}", 'admin', 'admin123', System.out, true)

      JiraFieldMappingUtil jiraFieldMappingUtil = new JiraFieldMappingUtil(jiraNotificationCustomFieldMapTest, client, mockRun.getEnvironment(mockListener), mockLogger)

      jiraFieldMappingUtil.expandEnvVars()
      jiraFieldMappingUtil.assignFieldsFromConfig()
      jiraFieldMappingUtil.mapCustomFieldNamesToIds()

    expect:
      jiraFieldMappingUtil.applicationCustomFieldId == "customfield_10200"
  }
}
