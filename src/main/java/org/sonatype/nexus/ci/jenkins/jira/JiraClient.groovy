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

import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class JiraClient
{
  static String USER_AGENT = 'nexus-jenkins-notifier'

  static String LOGO_URL = 'http://cdn.sonatype.com/brand/logo/nexus-iq-64-no-background.png'

  static String INSIGHT_KEY = 'sonatype-nexus-iq'

  SonatypeHTTPBuilder http

  String serverUrl

  String username

  String password

  JiraClient(String serverUrl, String username, String password) {
    this.http = new SonatypeHTTPBuilder()
    this.serverUrl = serverUrl
    this.username = username
    this.password = password
  }

  def createIssue(projectKey, description, detail, source, severity, fprint) {
    def url = getCreateIssueRequestUrl(serverUrl)
    def body = getCreateIssueRequestBody(projectKey, description, detail, source, severity, fprint)
    def headers = getRequestHeaders(username, password)

    http.post(url, body, headers)
  }

  def lookupJiraTickets(String projectKey)
  {
    def url = getLookupTicketsForProjectUrl(serverUrl, projectKey)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def closeTicket(String ticketInternalId)
  {
    //1. Get the transitions
    def url = getIssueTransitionsUrl(serverUrl, ticketInternalId)
    def headers = getRequestHeaders(username, password)
    def resp = http.get(url, headers)

    //2. Pick the right transition
    //todo : default
    def transition_id = "21"

    resp.transitions.each {
      if ("In Progress".equals(it.name)){
        transition_id = it.id
      }
    }

    //3. Issue the transition
    url = getExecuteTransitionUrl(serverUrl, ticketInternalId)
    def body = getExecuteTransitionRequestBody(transition_id)
    http.post(url, body, headers)
  }

  private static Map getExecuteTransitionRequestBody(String transitionId) {
    return [
            transition : [
                    id: transitionId
            ]
    ]
  }

  private static String getExecuteTransitionUrl(String serverUrl, String ticketInternalId)
  {
    //POST: /rest/api/2/issue/10110/transitions - [{"transition":{"id":"21"}}]
    return "${serverUrl}/rest/api/2/issue/${ticketInternalId}/transitions"

  }

  private static String getIssueTransitionsUrl(String serverUrl, String ticketInternalId)
  {
    // "/rest/api/2/issue/{{ticketInternalId}}/transitions?expand=transitions.fields"
    return "${serverUrl}/rest/api/2/issue/${ticketInternalId}/transitions?expand=transitions.fields"

  }

  private static Map getRequestHeaders(username, password) {
    return [
            'User-Agent' : USER_AGENT,
            Authorization: 'Basic ' + ("${username}:${password}").bytes.encodeBase64()
    ]
  }

  private static String getLookupTicketsForProjectUrl(serverUrl, projectKey) {
    // "/rest/api/2/search?jql=project%3D%22{{jiraProjectKey}}%22"
    return "${serverUrl}/rest/api/2/search?jql=project%3D%22${projectKey}%22"
  }


  private static String getCreateIssueRequestUrl(serverUrl) {
    //post: /rest/api/2/issue - [{"fields":{"project":{"key":"DP"},"summary":"Sonatype IQ Server SECURITY-HIGH Policy Violation","description":"\n\tDescription: Sonatype IQ Server SECURITY-HIGH Policy Violation\n\n\tTimestamp: 2019-01-26 01:38:59 -0500\n\n\tSource: SonatypeIQ:IQServerAppId:scanIQ\n\n\tSeverity: 1\n\n\tFingerprint:  57767fa9ecbe0b6271f20ea215e969ac7ed8f24ff7a67ee77dbf090e9e7f469b\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  CVE-2019-1234",
    // "priority":{"name":"Low"},"issuetype":{"name":"Bug"}},"labels":["Glue","triage.git"]}]
    return "${serverUrl}/rest/api/2/issue"
  }

  private static Map getCreateIssueRequestBody(projectKey, description, detail, source, severity, fprint) {
    //TODO: Fully Dynamic

    String newdate = new Date().format("YYYY-MM-DD HH:mm:ss Z") //2019-01-26 01:38:59 -0500
    //DateTimeFormatter f = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL )
    //String newdate = new Date().format(f)


    String formatted_description = "\n\tDescription: ${description}\n\n\tTimestamp: ${newdate}\n\n\tSource: ${source}\n\n\tSeverity: ${severity}\n\n\tFingerprint:  ${fprint}\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  ${detail}"

    return [
            fields : [
                    project: [
                            key: projectKey
                    ],
                    summary: "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                    description: formatted_description,
                    priority: [
                            name: "Low"
                    ],
                    issuetype: [
                            name: "Bug"
                    ]
                    ],
            labels: [
                    "Glue",
                    "triage.git"
            ]
           ]
  }
}
