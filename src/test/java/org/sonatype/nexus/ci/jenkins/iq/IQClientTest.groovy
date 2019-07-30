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

import groovy.json.JsonSlurper
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import org.sonatype.nexus.ci.jenkins.model.IQVersionRecommendation
import spock.lang.Requires
import spock.lang.Specification

class IQClientTest
    extends Specification
{
  //private static final String port = "60359" //for Charles Proxy
  private static final String port = "8060"

  SonatypeHTTPBuilder http
  IQClient clientHttpMock, clientLive
  def iqApplication, iqOrganizations, iqApplications, iqReportLinks
  def iqVersionRecommendationEmpty, iqVersionRecommendationDifferent, iqVersionRecommendationSame, iqVersionRecommendationOnlyNonFail, iqVersionRecommendationOnlyNoViolations
  String dateQualifier = "As of " + new Date().format("yyyy-MM-dd") + ":"


  private static final String iqTestAppExternalId = "aaaaaaa-testidegrandfathering"
  private static final String iqTestAppInternalId = "e06a119c75d04d97b8d8c11b62719752"

  private static final String iqTestReportInternalId = "e8ef4d3d26dd48b3866019b1478c6453" //demo (69)


  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    clientHttpMock = new IQClient("http://localhost:${port}/iq", 'admin', 'admin123', System.out, true)
    clientHttpMock.http = http

    iqApplication = new JsonSlurper().parse(new File("src/test/resources/iq-${iqTestAppExternalId}-applicationInfo.json"))
    iqOrganizations = new JsonSlurper().parse(new File('src/test/resources/iq-organizations.json'))
    iqApplications = new JsonSlurper().parse(new File('src/test/resources/iq-applications.json'))
    iqReportLinks = new JsonSlurper().parse(new File('src/test/resources/iq-report-links.json'))
    iqVersionRecommendationEmpty = new JsonSlurper().parse(new File('src/test/resources/iq-version-recommendation-empty.json'))
    iqVersionRecommendationDifferent = new JsonSlurper().parse(new File('src/test/resources/iq-version-recommendation-different-nofail-noviolation.json'))
    iqVersionRecommendationSame = new JsonSlurper().parse(new File('src/test/resources/iq-version-recommendation-same-nofail-noviolation.json'))
    iqVersionRecommendationOnlyNonFail = new JsonSlurper().parse(new File('src/test/resources/iq-version-recommendation-only-non-failing.json'))
    iqVersionRecommendationOnlyNoViolations = new JsonSlurper().parse(new File('src/test/resources/iq-version-recommendation-only-no-violations.json'))

    clientLive = Spy(IQClient, constructorArgs: ["http://localhost:${port}/iq", 'admin', 'admin123', System.out, true])
  }

  def 'lookup scan report policy threats has correct url'() {
    def url

    when:
      clientHttpMock.lookupPolcyDetailsFromIQ("a22d44d0209b47358c8dd2532bb7afb3", iqTestAppExternalId)

    then:
      1 * http.get(_, _) >> { args -> url = args[0]}

    and:
      url != null
      url == "http://localhost:${port}/iq/rest/report/${iqTestAppExternalId}/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json"
  }

  def 'lookup scan report component details has correct url'() {
    def url

    when:
    clientHttpMock.lookupComponentDetailsFromIQ("a22d44d0209b47358c8dd2532bb7afb3", iqTestAppExternalId)

    then:
    1 * http.get(_, _) >> { args -> url = args[0]}

    and:
    url != null
    url == "http://localhost:${port}/iq/api/v2/applications/${iqTestAppExternalId}/reports/a22d44d0209b47358c8dd2532bb7afb3"
  }

  def 'Lookup Application'() {
    when:
    def applicationResp = clientHttpMock.lookupApplication(iqTestAppExternalId )

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications/?publicId=${iqTestAppExternalId}", _) >> iqApplication

    applicationResp != null
    applicationResp.applications.size == 1
    applicationResp.applications[0].publicId == iqTestAppExternalId
    applicationResp.applications[0].id == iqTestAppInternalId
    applicationResp.applications[0].organizationId == "2d0dfb87b67b43a3b4271e462bae9eca"
  }

  def 'Lookup Organization Name for App Id'() {
    when:
    String orgName = clientHttpMock.lookupOrganizationName(iqTestAppExternalId)

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications/?publicId=${iqTestAppExternalId}", _) >> iqApplication
    1 * http.get("http://localhost:${port}/iq/api/v2/organizations", _) >> iqOrganizations

    and:
    orgName == "Automatically Created Apps"
  }

  def 'Lookup Organization Name for App Id - Empty Org List returns Null'() {
    setup:
    def iqEmptyOrganizations = new JsonSlurper().parse(new File('src/test/resources/iq-organizations-empty.json'))

    when:
    String orgName = clientHttpMock.lookupOrganizationName(iqTestAppExternalId)

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications/?publicId=${iqTestAppExternalId}", _) >> iqApplication
    1 * http.get("http://localhost:${port}/iq/api/v2/organizations", _) >> iqEmptyOrganizations

    and:
    orgName == null
  }

  def 'Lookup All Applications'() {
    when:
    def resp = clientHttpMock.lookupApplications()

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications", _) >> iqApplications

    and:
    resp.applications.size == 102
  }

  def 'Lookup Report Links'() {
    when:
    def resp = clientHttpMock.lookupApplicationReportLinks(iqTestAppInternalId)

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/reports/applications/${iqTestAppInternalId}", _) >> iqReportLinks

    and:
    resp.size == 4
  }

  def 'Lookup Report Link for Stage'() {
    when:
    def resp = clientHttpMock.lookupReportLink(iqTestAppExternalId, stageName)

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications/?publicId=${iqTestAppExternalId}", _) >> iqApplication
    1 * http.get("http://localhost:${port}/iq/api/v2/reports/applications/${iqTestAppInternalId}", _) >> iqReportLinks

    and:
    resp == responseUrl

    where:
    stageName | responseUrl
    "build"                 | "http://localhost:${port}/iq/ui/links/application/${iqTestAppExternalId}/report/6354f0c8f2bf4e2aa52aacf3177c5630"
    "stage-release"         | "http://localhost:${port}/iq/ui/links/application/${iqTestAppExternalId}/report/${iqTestReportInternalId}"
    "release"               | "http://localhost:${port}/iq/ui/links/application/${iqTestAppExternalId}/report/729af8111f1f47e2988b4c2020f3e92f"
    "operate"               | "http://localhost:${port}/iq/ui/links/application/${iqTestAppExternalId}/report/1a017b0f54e04ac388f580ad7378893f"
    "foobar"                | null
  }

  def 'Lookup Stage for Report Link'() {
    when:
    def resp = clientHttpMock.lookupStageForReport(iqTestAppExternalId, reportInternalId)

    then:
    1 * http.get("http://localhost:${port}/iq/api/v2/applications/?publicId=${iqTestAppExternalId}", _) >> iqApplication
    1 * http.get("http://localhost:${port}/iq/api/v2/reports/applications/${iqTestAppInternalId}", _) >> iqReportLinks

    and:
    resp == stageName

    where:
    reportInternalId | stageName
    "6354f0c8f2bf4e2aa52aacf3177c5630" | "build"
    iqTestReportInternalId             | "stage-release"
    "729af8111f1f47e2988b4c2020f3e92f" | "release"
    "1a017b0f54e04ac388f580ad7378893f" | "operate"
    "xyz"                              | null
  }

  def 'Lookup Remediation Recommendation - Returns No Recommendation'() {
    when:
    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"

    IQVersionRecommendation resp = new IQVersionRecommendation(clientHttpMock.lookupRecommendedVersion(purl,"release", iqTestAppInternalId), "release")

    then:
    1 * http.post("http://localhost:${port}/iq/api/v2/components/remediation/application/${iqTestAppInternalId}?stageId=release", _, _) >> iqVersionRecommendationEmpty

    and:
    resp != null
    resp.data != null
    resp.getRecommendationText("4.5.1") == "${dateQualifier}\n\t* No recommended versions are available for the current component"
  }

  def 'Lookup Remediation Recommendation - Old Version Returns both NoFail/NoViolation and Current Version Returns Current Recommendation'() {
    when:
    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"

    IQVersionRecommendation resp = new IQVersionRecommendation(clientHttpMock.lookupRecommendedVersion(purl,"release", iqTestAppInternalId), "release")

    then:
    1 * http.post("http://localhost:${port}/iq/api/v2/components/remediation/application/${iqTestAppInternalId}?stageId=release", _, _) >> iqVersionRecommendationSame

    and:
    resp != null
    resp.data != null
    resp.getRecommendationText("4.5.1") == "${dateQualifier}\n\t* 4.5.3: Next version with no failing policy violations also has no violations"
    resp.getRecommendationText("4.5.3") == "${dateQualifier}\n\t* Current version has no policy violations"
  }

  def 'Lookup Remediation Recommendation - Different Version Recommendations return both versions'() {
    when:
    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"

    IQVersionRecommendation resp = new IQVersionRecommendation(clientHttpMock.lookupRecommendedVersion(purl,"release", iqTestAppInternalId), "release")

    then:
    1 * http.post("http://localhost:${port}/iq/api/v2/components/remediation/application/${iqTestAppInternalId}?stageId=release", _, _) >> iqVersionRecommendationDifferent

    and:
    resp != null
    resp.data != null
    resp.getRecommendationText("4.5.1") == "${dateQualifier}\n\t* 4.5.2: Next version with no failing policy when evaluated at Nexus IQ Server scan stage: release\n\t* 4.5.3: Next version with no policy violations"
    resp.getRecommendationText("4.5.2") == "${dateQualifier}\n\t* Current version has no failing violations when evaluated at Nexus IQ Server scan stage: release\n\t* 4.5.3: Next version with no policy violations"
  }

  def 'Lookup Remediation Recommendation - Only Non-Fail version available'() {
    when:
    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"

    IQVersionRecommendation resp = new IQVersionRecommendation(clientHttpMock.lookupRecommendedVersion(purl,"release", iqTestAppInternalId), "release")

    then:
    1 * http.post("http://localhost:${port}/iq/api/v2/components/remediation/application/${iqTestAppInternalId}?stageId=release", _, _) >> iqVersionRecommendationOnlyNonFail

    and:
    resp != null
    resp.data != null
    resp.getRecommendationText("4.5.1") == "${dateQualifier}\n\t* 4.5.2: Next version with no failing policy when evaluated at Nexus IQ Server scan stage: release\n\t* No recommended version clean of policy violations"
    resp.getRecommendationText("4.5.2") == "${dateQualifier}\n\t* Current version has no failing violations when evaluated at Nexus IQ Server scan stage: release\n\t* No recommended version clean of policy violations"
  }

  def 'Lookup Remediation Recommendation - Only No-Violations version available'() {
    when:
    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"

    IQVersionRecommendation resp = new IQVersionRecommendation(clientHttpMock.lookupRecommendedVersion(purl,"release", iqTestAppInternalId), "release")

    then:
    1 * http.post("http://localhost:${port}/iq/api/v2/components/remediation/application/${iqTestAppInternalId}?stageId=release", _, _) >> iqVersionRecommendationOnlyNoViolations

    and:
    resp != null
    resp.data != null
    resp.getRecommendationText("4.5.1") == "${dateQualifier}\n\t* 4.5.3: Next version with no policy violations"
    resp.getRecommendationText("4.5.3") == "${dateQualifier}\n\t* Current version has no policy violations"
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Get Report Violations'() {
    when:
    def resp = clientLive.lookupPolcyDetailsFromIQ(iqTestReportInternalId, iqTestAppExternalId)
    //a go-lang report = def resp = clientLive.lookupPolcyDetailsFromIQ("f3e278dbee1d417aa34909513e089e6d", "go-demo")

    then:
      resp != null
      resp.aaData.size > 0
      resp.aaData[0].hash != null
      resp.aaData[0].activeViolations.size == 2
      resp.aaData[0].waivedViolations.size == 1
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Get Report Component Details'() {
    when:
    def resp = clientLive.lookupComponentDetailsFromIQ(iqTestReportInternalId, iqTestAppExternalId)

    then:
    resp != null
    resp.components.size > 0
    resp.components[0].componentIdentifier != null
    resp.components[0].hash != null
    resp.components[0].licenseData != null
    resp.components[0].pathnames != null
    resp.components[0].securityData != null
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Organization for App Id'() {
    when:
      def applicationResp = clientLive.lookupApplication(iqTestAppExternalId )
      def organizationsResp = clientLive.lookupOrganizations()

    then:
      applicationResp != null
      applicationResp.applications.size == 1
      applicationResp.applications[0].publicId == iqTestAppExternalId
      applicationResp.applications[0].id == iqTestAppInternalId
      applicationResp.applications[0].organizationId == "2d0dfb87b67b43a3b4271e462bae9eca"
      organizationsResp != null
      organizationsResp.organizations.size > 0
      organizationsResp.organizations.find { it.id == applicationResp.applications[0].organizationId }.name == "Automatically Created Apps"
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Organization Name for App Id'() {
    when:
      def resp = clientLive.lookupOrganizationName(iqTestAppExternalId )

    then:
      resp == "Automatically Created Apps"
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Applications'() {
    when:
      def resp = clientLive.lookupApplications()

    then:
      resp.applications.size != 0
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Report Links'() {
    when:
    def resp = clientLive.lookupApplicationReportLinks(iqTestAppInternalId)

    then:
    resp.size != 0
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Remediation Recommendation'() {
    String appId = iqTestAppInternalId
    appId = "a5004e5c189349faac376647f8863a19"

    String purl = "pkg:maven/org.apache.httpcomponents/httpclient@4.5.1?type=jar"
    //purl = "pkg:maven/axis/axis@1.4?type=jar"

    when:
    IQVersionRecommendation resp = new IQVersionRecommendation(clientLive.lookupRecommendedVersion(purl,"release", appId), "release")

    then:
    resp != null
    resp.data != null

  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Product Version'() {

    when:
    boolean before = clientLive.isVersionSupported("1.59")
    boolean thisVersion = clientLive.isVersionSupported("1.69")
    boolean after = clientLive.isVersionSupported("1.70")

    then:
    before == true
    thisVersion == true
    after == false
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup CVE Details'() {

    when:
    def resp = clientLive.lookupCweAndThreatVector(cveCode)

    then:
    resp == result

    where:
    cveCode | result
    "sonatype-2019-0001" | ["94", "CVSS:3.0/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H"]
    "sonatype-2016-0030" | ["79", ""]
    "CVE-2019-9047" |  ["89", "CVSS:3.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H"]
  }
}
