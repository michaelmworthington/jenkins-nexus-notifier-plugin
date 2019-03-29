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

import org.sonatype.nexus.ci.jenkins.util.AbstractToolClient

class IQClient extends AbstractToolClient
{
  IQClient(String serverUrl, String username, String password, PrintStream logger, final boolean verboseLogging = false) {
    super(serverUrl, username, password, logger, verboseLogging)
  }

  def lookupPolcyDetailsFromIQ(String iqReportInternalid, String iqAppExternalId)
  {
    def url = getPolicyEvaluationResultsUrl(serverUrl, iqAppExternalId, iqReportInternalid)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  private String getPolicyEvaluationResultsUrl(String serverUrl, String iqAppExternalId, String iqReportInternalid) {
    verbosePrintLn("Get the Application Policy Threats Report from IQ Server")

    // /rest/report/{{iqAppExternalId}}/{{iqReportInternalId}}/browseReport/policythreats.json
    return "${serverUrl}/rest/report/${iqAppExternalId}/${iqReportInternalid}/browseReport/policythreats.json"
  }
}
