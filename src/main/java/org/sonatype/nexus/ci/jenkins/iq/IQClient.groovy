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
package org.sonatype.nexus.ci.jenkins.iq

import groovyx.net.http.HttpResponseException
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder

class IQClient
{
  static String USER_AGENT = 'nexus-jenkins-notifier'

  SonatypeHTTPBuilder http

  String serverUrl

  String username

  String password

  IQClient(String serverUrl, String username, String password, PrintStream logger) {
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
        throw new HttpResponseException(resp)
      }
    }
  }

  def lookupPolcyDetailsFromIQ(String iqReportInternalid, String iqAppExternalId)
  {
    def url = getPolicyEvaluationResultsUrl(serverUrl, iqAppExternalId, iqReportInternalid)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  private static Map getRequestHeaders(username, password) {
    return [
            'User-Agent' : USER_AGENT,
            Authorization: 'Basic ' + ("${username}:${password}").bytes.encodeBase64()
    ]
  }

  private static String getPolicyEvaluationResultsUrl(String serverUrl, String iqAppExternalId, String iqReportInternalid) {
    // /rest/report/{{iqAppExternalId}}/{{iqReportInternalId}}/browseReport/policythreats.json
    return "${serverUrl}/rest/report/${iqAppExternalId}/${iqReportInternalid}/browseReport/policythreats.json"
  }

}
