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

  def lookupReportLink(String iqAppExternalId, String iqStage)
  {
    def applicationResp = lookupApplication(iqAppExternalId)
    def reportsResp = lookupApplicationReportLinks(applicationResp?.applications[0]?.id)
    def reportHtmlUrl = reportsResp.find { it.stage == iqStage }?.reportHtmlUrl

    if (reportHtmlUrl)
    {
      return "${serverUrl}/${reportHtmlUrl}"
    }
  }

  private def lookupApplicationReportLinks(String iqAppInternalId)
  {
    def url = getApplicationReportLinksUrl(serverUrl, iqAppInternalId)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  private def lookupApplication(String iqAppExternalId)
  {
    def url = getApplicationUrl(serverUrl, iqAppExternalId)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  private def lookupOrganizations()
  {
    def url = getOrganizationsUrl(serverUrl)
    def headers = getRequestHeaders(username, password)

    http.get(url, headers)
  }

  def lookupApplications()
  {
    def url = getApplicationsUrl(serverUrl)
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
    return "${serverUrl}/rest/report/${iqAppExternalId}/${iqReportInternalId}/browseReport/policythreats.json" //TODO: change to the official "Policy" & "Raw" APIs
  }

  private String getApplicationReportLinksUrl(String serverUrl, String iqAppInternalId) {
    verbosePrintLn("Get the Application Report Links from IQ Server")

    //{{iqURL}}/api/v2/reports/applications/{{iqAppInternalId}}
    return "${serverUrl}/api/v2/reports/applications/${iqAppInternalId}"
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

  private String getApplicationsUrl(String serverUrl) {
    verbosePrintLn("Get the Application Details from IQ Server")

    return "${serverUrl}/api/v2/applications"
  }

  private String getComponentDetailsReportUrl(String serverUrl, String iqAppExternalId, String iqReportInternalId) {
    verbosePrintLn("Get the Application Component Details Report from IQ Server")

    // <=64 policythreats.json + reports/id
    // 65<= reports/id/policy  + reports/id/raw
    // {{iqURL}}/api/v2/applications/{{iqAppExternalId}}/reports/{{iqReportInternalId}}

    //todo: raw = new in 65
    //todo: new in 67 = 		 "packageUrl": "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
    return "${serverUrl}/api/v2/applications/${iqAppExternalId}/reports/${iqReportInternalId}/raw"
  }

  private String getPolicyDetailsReportUrl(String serverUrl, String iqAppExternalId, String iqReportInternalId)
  {
    verbosePrintLn("Get the Application Component Details Report from IQ Server")

    //todo: policy = new in 65
    //todo: new in 67 = 		 "packageUrl": "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
    return "${serverUrl}/api/v2/applications/${iqAppExternalId}/reports/${iqReportInternalId}/policy"
  }

  private String getComponentRemediationDetailsUrl(String serverUrl, String iqAppExternalId, String stageId)
  {
    verbosePrintLn("Get the Component Remediation Details from IQ Server")

    //https://help.sonatype.com/iqserver/automating/rest-apis/component-remediation-rest-api---v2
    //POST /api/v2/components/remediation/application/{applicationInternalId}?stageId={stageId}

    //todo: new in 64
    //todo:        64: next-no-violations
    //todo:        64: next-non-failing
    //todo:  if no version satisfies, returns null / empty array
    //todo:  if the current version satisfies, returns the curent version
    //todo: new in 67 = 		 "packageUrl": "pkg:maven/tomcat/tomcat-util@5.5.23?type=jar",
    return "${serverUrl}/api/v2/components/remediation/application/${applicationInternalId}?stageId=${stageId}"
  }

  private String getCVELinkUrl(String serverUrl) {
    verbosePrintLn("Get the CVE Link from IQ Server")

    // /iq/rest/vulnerability/details/sonatype/sonatype-2016-0030?hash=c8f6dcb0732868e7e5c2&componentIdentifier=%7B%22format%22%3A%22a-name%22%2C%22coordinates%22%3A%7B%22name%22%3A%22marked%22%2C%22qualifier%22%3A%22%22%2C%22version%22%3A%220.3.6%22%7D%7D
    // {"refId":"sonatype-2016-0030","source":"sonatype","htmlDetails":"<style> dt { color: #070707; font-weight: bold; margin-top: 18px; margin-bottom: 4px;} dt:first-of-type { margin-top: 0;} p:first-of-type { margin-top: 0;} </style>\n<div id=\"hds-sd\" class=\"iq-grid-row\">\n  <div class=\"iq-grid-col iq-grid-col--25\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Vulnerability</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability\">\n<dt>\nIssue\n</dt>\n<dd>\nsonatype-2016-0030\n</dd>\n<dt>\nSeverity\n</dt>\n<dd>\nSonatype CVSS 3.0: 6.1\n</dd>\n<dt>\nWeakness\n</dt>\n<dd>\nSonatype CWE: <a target=\"_blank\" href=\"https://cwe.mitre.org/data/definitions/79.html\">79</a>\n</dd>\n<dt>\nSource\n</dt>\n<dd>\nSonatype Data Research\n</dd>\n<dt>\nCategories\n</dt>\n<dd>\nData\n</dd>\n    </dl>\n  </div>\n  <div class=\"iq-grid-col\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Description</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability-description\">\n<dt>\nExplanation\n</dt>\n<dd>\n<p>The marked package is vulnerable to Cross-Site Scripting (XSS). The <code>unescape</code> function in the <code>marked.js</code> file fails to decode certain user-supplied characters. These still-encoded characters are then ignored when the package tries to sanitize the input. An attacker can inject malicious encoded JavaScript into markdown and submit that markdown to this package. This package will render that markdown, including the malicious JavaScript, as HTML.</p>\n<p>Note: This vulnerability has been assigned CVE-2016-10531.</p>\n\n</dd>\n<dt>\nDetection\n</dt>\n<dd>\n<p>The application is vulnerable by using this component.</p>\n\n</dd>\n<dt>\nRecommendation\n</dt>\n<dd>\n<p>We recommend upgrading to a version of this component that is not vulnerable to this specific issue.</p>\n\n</dd>\n<dt>\nRoot Cause\n</dt>\n<dd>\norg.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6) <br> org.wso2.carbon.apimgt.rest.api.store-6.3.30.war <b>&lt;=</b> org.wso2.carbon.apimgt.rest.api.util-6.3.30.jar <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6)\n</dd>\n<dt>\nAdvisories\n</dt>\n<dd>\nProject: <a href=\"https://github.com/markedjs/marked/pull/592\" target=\"_blank\">https://github.com/markedjs/marked/pull/592</a>\n</dd>\n<dt>\nCVSS Details\n</dt>\n<dd>\nSonatype CVSS 3.0: 6.1\n</dd>\n    </dl>\n  </div>\n</div>\n"}
    // {"refId":"sonatype-2016-0030","source":"sonatype","htmlDetails":"<style> dt { color: #070707; font-weight: bold; margin-top: 18px; margin-bottom: 4px;} dt:first-of-type { margin-top: 0;} p:first-of-type { margin-top: 0;} </style>\n<div id=\"hds-sd\" class=\"iq-grid-row\">\n  <div class=\"iq-grid-col iq-grid-col--25\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Vulnerability</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability\">\n<dt>\nIssue\n</dt>\n<dd>\nsonatype-2016-0030\n</dd>\n<dt>\nSeverity\n</dt>\n<dd>\nSonatype CVSS 3.0: 6.1\n</dd>\n<dt>\nWeakness\n</dt>\n<dd>\nSonatype CWE: <a target=\"_blank\" href=\"https://cwe.mitre.org/data/definitions/79.html\">79</a>\n</dd>\n<dt>\nSource\n</dt>\n<dd>\nSonatype Data Research\n</dd>\n<dt>\nCategories\n</dt>\n<dd>\nData\n</dd>\n    </dl>\n  </div>\n  <div class=\"iq-grid-col\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Description</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability-description\">\n<dt>\nExplanation\n</dt>\n<dd>\n<p>The marked package is vulnerable to Cross-Site Scripting (XSS). The <code>unescape</code> function in the <code>marked.js</code> file fails to decode certain user-supplied characters. These still-encoded characters are then ignored when the package tries to sanitize the input. An attacker can inject malicious encoded JavaScript into markdown and submit that markdown to this package. This package will render that markdown, including the malicious JavaScript, as HTML.</p>\n<p>Note: This vulnerability has been assigned CVE-2016-10531.</p>\n\n</dd>\n<dt>\nDetection\n</dt>\n<dd>\n<p>The application is vulnerable by using this component.</p>\n\n</dd>\n<dt>\nRecommendation\n</dt>\n<dd>\n<p>We recommend upgrading to a version of this component that is not vulnerable to this specific issue.</p>\n\n</dd>\n<dt>\nRoot Cause\n</dt>\n<dd>\nmarked-0.3.6.tgz <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> marked.0.3.6.tgz <b>&lt;=</b> marked.min.js : ( , 0.3.6) <br> marked-0.3.6.tgz <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6) <br> marked.0.3.6.tgz <b>&lt;=</b> marked.min.js : [0.3.3, 0.3.6)\n</dd>\n<dt>\nAdvisories\n</dt>\n<dd>\nProject: <a href=\"https://github.com/markedjs/marked/pull/592\" target=\"_blank\">https://github.com/markedjs/marked/pull/592</a>\n</dd>\n<dt>\nCVSS Details\n</dt>\n<dd>\nSonatype CVSS 3.0: 6.1\n</dd>\n    </dl>\n  </div>\n</div>\n"}
    //todo: what is difference between hash/coordinates and not

    // /iq/rest/vulnerability/details/sonatype/sonatype-2019-0001
    // {"refId":"sonatype-2019-0001","source":"sonatype","htmlDetails":"<style> dt { color: #070707; font-weight: bold; margin-top: 18px; margin-bottom: 4px;} dt:first-of-type { margin-top: 0;} p:first-of-type { margin-top: 0;} </style>\n<div id=\"hds-sd\" class=\"iq-grid-row\">\n  <div class=\"iq-grid-col iq-grid-col--25\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Vulnerability</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability\">\n<dt>\nIssue\n</dt>\n<dd>\nsonatype-2019-0001\n</dd>\n<dt>\nSeverity\n</dt>\n<dd>\nSonatype CVSS 3.0: 8.8\n</dd>\n<dt>\nWeakness\n</dt>\n<dd>\nSonatype CWE: <a target=\"_blank\" href=\"https://cwe.mitre.org/data/definitions/94.html\">94</a>\n</dd>\n<dt>\nSource\n</dt>\n<dd>\nSonatype Data Research\n</dd>\n<dt>\nCategories\n</dt>\n<dd>\nData\n</dd>\n    </dl>\n  </div>\n  <div class=\"iq-grid-col\">\n    <div class=\"iq-grid-header\">\n      <h2 class=\"iq-grid-header__title\">Description</h2>\n      <hr class=\"iq-grid-header__hrule\">\n    </div>\n    <dl class=\"vulnerability-description\">\n<dt>\nDescription from Sonatype\n</dt>\n<dd>\nconsul - Remote Command Execution via Rexec &#x28;Metasploit&#x29;&#x9;\n</dd>\n<dt>\nRoot Cause\n</dt>\n<dd>\nconsul-0-0.1.git5079177.el6.i686.rpm : ( , )\n</dd>\n<dt>\nAdvisories\n</dt>\n<dd>\nAttack: <a href=\"https://www.exploit-db.com/exploits/46073\" target=\"_blank\">https://www.exploit-db.com/exploits/46073</a>\n</dd>\n<dt>\nCVSS Details\n</dt>\n<dd>\nSonatype CVSS 3.0: 8.8 <br> CVSS Vector: CVSS:3.0/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H\n</dd>\n    </dl>\n  </div>\n</div>\n"}
    //todo: cwe
    // todo: CVSS Vector


    // http://localhost:8060/iq/ui/links/vln/sonatype-2017-0367
    // https://my.sonatype.com/nexus-intelligence/sonatype-2017-0367

    return "${serverUrl}/api/v2/applications"
  }

}
