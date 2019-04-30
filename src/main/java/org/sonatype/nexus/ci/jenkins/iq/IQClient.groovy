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

  def lookupApplication(String iqAppExternalId)
  {
    def url = getApplicationUrl(serverUrl, iqAppExternalId)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupOrganizations()
  {
    def url = getOrganizationsUrl(serverUrl)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupComponentDetailsFromIQ(String iqReportInternalid, String iqAppExternalId)
  {
    def url = getComponentDetailsReportUrl(serverUrl, iqAppExternalId, iqReportInternalid)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupOrganizationName(String iqAppExternalId)
  {
    def applicationResp = lookupApplication(iqAppExternalId)
    def organizationsResp = lookupOrganizations()
    return organizationsResp.organizations.find { it.id == applicationResp?.applications[0]?.organizationId }?.name
  }

  private String getPolicyEvaluationResultsUrl(String serverUrl, String iqAppExternalId, String iqReportInternalId) {
    verbosePrintLn("Get the Application Policy Threats Report from IQ Server")

    // /rest/report/{{iqAppExternalId}}/{{iqReportInternalId}}/browseReport/policythreats.json
    return "${serverUrl}/rest/report/${iqAppExternalId}/${iqReportInternalId}/browseReport/policythreats.json"
  }

  private String getApplicationUrl(String serverUrl, String iqAppExternalId) {
    verbosePrintLn("Get the Application Details from IQ Server")

    //{{iqURL}}/api/v2/applications?publicId={{iqAppExternalId}}
    return "${serverUrl}/api/v2/applications/?publicId=${iqAppExternalId}"
  }

  private String getOrganizationsUrl(String serverUrl) {
    verbosePrintLn("Get the Organization Details from IQ Server")

    return "${serverUrl}/api/v2/organizations"
  }

  private String getComponentDetailsReportUrl(String serverUrl, String iqAppExternalId, String iqReportInternalId) {
    verbosePrintLn("Get the Application Component Details Report from IQ Server")

    // {{iqURL}}/api/v2/applications/{{iqAppExternalId}}/reports/{{iqReportInternalId}}
    return "${serverUrl}/api/v2/applications/${iqAppExternalId}/reports/${iqReportInternalId}/raw"
  }
}
