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

import groovyx.net.http.HttpResponseException
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder

class JiraClient
{
  static String USER_AGENT = 'nexus-jenkins-notifier'

  static String LOGO_URL = 'http://cdn.sonatype.com/brand/logo/nexus-iq-64-no-background.png'

  static String INSIGHT_KEY = 'sonatype-nexus-iq'

  SonatypeHTTPBuilder http

  String serverUrl

  String username

  String password

  JiraClient(String serverUrl, String username, String password, PrintStream logger) {
    this.http = new SonatypeHTTPBuilder()
    this.serverUrl = serverUrl
    this.username = username
    this.password = password

    if(logger)
    {
      /*
       * Jira sends the error message in the body. Let's print that out in addition to the stack trace
       *   ref: https://stackoverflow.com/questions/19966548/groovy-httpbuilder-get-body-of-failed-response
       */
      this.http.handler.failure = { resp, reader ->
        logger.println("Error Response Code: ${resp.status} with message: ${reader}")
        //[response:resp, reader:reader]
        throw new HttpResponseException(resp);
      }
    }
  }

  def createIssue(projectKey,
                  description,
                  detail,
                  source,
                  severity,
                  fprint,
                  iqAppExternalId,
                  String iqAppExternalIdCustomFieldId,
                  String iqOrgExternalId,
                  String iqOrgExternalIdCustomFieldId) {
    def url = getCreateIssueRequestUrl(serverUrl)
    def body = getCreateIssueRequestBody(projectKey, description, detail, source, severity, fprint, iqAppExternalId, iqAppExternalIdCustomFieldId,iqOrgExternalId,iqOrgExternalIdCustomFieldId)
    def headers = getRequestHeaders(username, password)

    http.post(url, body, headers)
  }

  def lookupJiraTickets(String projectKey,
                        String transitionTargetStatus,
                        String applicationCustomFieldName,
                        String iqApplicationExternalId)
  {
    def url = getLookupTicketsForProjectUrl(serverUrl, projectKey, transitionTargetStatus, applicationCustomFieldName, iqApplicationExternalId)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupCustomFields()
  {
    def url = getLookupCustomFieldsUrl(serverUrl)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def closeTicket(String ticketInternalId, String pTransitionName)
  {
    //1. Get the transitions
    def url = getIssueTransitionsUrl(serverUrl, ticketInternalId)
    def headers = getRequestHeaders(username, password)
    def resp = http.get(url, headers)

    //2. Pick the right transition
    //todo : what should the default be? Or, what to do if no transition matches?
    def transition_id = "21"

    resp.transitions.each {
      if (pTransitionName.equals(it.name)){
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

  private static String getLookupTicketsForProjectUrl(String serverUrl,
                                                      String projectKey,
                                                      String transitionTargetStatus,
                                                      String applicationCustomFieldName,
                                                      String iqApplicationExternalId)
  {
    // "/rest/api/2/search?jql=project%3D%22{{jiraProjectKey}}%22"
    // %20AND%20status!%3D%22Done%22
    // %20AND%20"IQ%20Application"%20~%20"test"

    StringBuffer plainSearchString = new StringBuffer();

    plainSearchString <<= "project = \"${projectKey}\"";

    if (transitionTargetStatus?.trim())
    {
      plainSearchString <<= " AND status != \"${transitionTargetStatus}\""
    }

    if (applicationCustomFieldName?.trim())
    {
      plainSearchString <<= " AND \"${applicationCustomFieldName}\" ~ \"${iqApplicationExternalId}\""
    }

    String urlSearchString = URLEncoder.encode(plainSearchString.toString(), "UTF-8")

    return "${serverUrl}/rest/api/2/search?jql=${urlSearchString}"

  }

  /**
   * Get the list of all fields in jira so we can map custom field name to id
   *
   *  https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-api-2-field-get
   *
   *  [
   *   {
   *     "id": "description",
   *     "name": "Description",
   *     "custom": false,
   *     "orderable": true,
   *     "navigable": true,
   *     "searchable": true,
   *     "clauseNames": [
   *       "description"
   *     ],
   *     "schema": {
   *       "type": "string",
   *       "system": "description"
   *     }
   *   },
   *   {
   *     "id": "summary",
   *     "key": "summary",
   *     "name": "Summary",
   *     "custom": false,
   *     "orderable": true,
   *     "navigable": true,
   *     "searchable": true,
   *     "clauseNames": [
   *       "summary"
   *     ],
   *     "schema": {
   *       "type": "string",
   *       "msary"
   *     }
   *   }
   * ]
   * @param serverUrl
   * @return
   */
  private static String getLookupCustomFieldsUrl(String serverUrl)
  {
    return "${serverUrl}/rest/api/2/field"
  }

  private static String getCreateIssueRequestUrl(serverUrl) {
    //post: /rest/api/2/issue - [{"fields":{"project":{"key":"DP"},"summary":"Sonatype IQ Server SECURITY-HIGH Policy Violation","description":"\n\tDescription: Sonatype IQ Server SECURITY-HIGH Policy Violation\n\n\tTimestamp: 2019-01-26 01:38:59 -0500\n\n\tSource: SonatypeIQ:IQServerAppId:scanIQ\n\n\tSeverity: 1\n\n\tFingerprint:  57767fa9ecbe0b6271f20ea215e969ac7ed8f24ff7a67ee77dbf090e9e7f469b\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  CVE-2019-1234",
    // "priority":{"name":"Low"},"issuetype":{"name":"Bug"}},"labels":["Glue","triage.git"]}]
    return "${serverUrl}/rest/api/2/issue"
  }

  private static Map getCreateIssueRequestBody(projectKey, description, detail, source, severity, fprint, iqAppExternalId,iqAppExternalIdCustomFieldId,iqOrgExternalId, iqOrgExternalIdCustomFieldId) {
    //TODO: Fully Dynamic

    String newdate = new Date().format("YYYY-MM-DD HH:mm:ss Z") //2019-01-26 01:38:59 -0500
    //DateTimeFormatter f = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL )
    //String newdate = new Date().format(f)

    String formatted_summary = "${description}"
    String formatted_description = "\n\tDescription: ${description}\n\n\tTimestamp: ${newdate}\n\n\tSource: ${source}\n\n\tSeverity: ${severity}\n\n\tFingerprint:  ${fprint}\n\n\tFound by:  SonatypeIQ\n\n\tDetail:  ${detail}"

    def returnValue = [
            fields : [
                      project: [
                              key: projectKey
                      ],
                      summary: formatted_summary,
                      description: formatted_description,
                      priority: [
                              name: "Low"
                      ],
                      issuetype: [
                              name: "Bug" //TODO: issue type
                      ]
                    ]
           ]

    if (iqAppExternalIdCustomFieldId && iqAppExternalId)
    {
      returnValue.fields.put(iqAppExternalIdCustomFieldId, iqAppExternalId)
    }

    if(iqOrgExternalIdCustomFieldId && iqOrgExternalId)
    {
      returnValue.fields.put(iqOrgExternalIdCustomFieldId, iqOrgExternalId)
    }

    return returnValue
  }
}
