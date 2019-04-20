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

import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import spock.lang.Requires
import spock.lang.Specification

class IQClientTest
    extends Specification
{
  //private static final String port = "60359" //for Charles Proxy
  private static final String port = "8060"

  SonatypeHTTPBuilder http
  IQClient clientHttpMock, clientLive

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    clientHttpMock = new IQClient("http://localhost:${port}/iq", 'admin', 'admin123', System.out, true)
    clientHttpMock.http = http

    clientLive = Spy(IQClient, constructorArgs: ["http://localhost:${port}/iq", 'admin', 'admin123', System.out, true])
  }

  def 'lookup scan report policy threats has correct url'() {
    def url

    when:
      clientHttpMock.lookupPolcyDetailsFromIQ("a22d44d0209b47358c8dd2532bb7afb3", "aaaaaaa-testidegrandfathering")

    then:
      1 * http.get(_, _) >> { args -> url = args[0]}

    and:
      url != null
      url == "http://localhost:${port}/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json"
  }

  /*
  ****************************************************************************************************************************************************
  *                                                     Integration Tests                                                                            *
  ****************************************************************************************************************************************************
   */

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Get Report Violations'() {
    when:
      def resp = clientLive.lookupPolcyDetailsFromIQ("e8ef4d3d26dd48b3866019b1478c6453", "aaaaaaa-testidegrandfathering")

    then:
      resp != null
      resp.aaData.size > 0
      resp.aaData[0].hash != null
      resp.aaData[0].activeViolations.size == 2
      resp.aaData[0].waivedViolations.size == 1
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Organization for App Id'() {
    when:
      def applicationResp = clientLive.lookupApplication("aaaaaaa-testidegrandfathering" )
      def organizationsResp = clientLive.lookupOrganizations()

    then:
      applicationResp != null
      applicationResp.applications.size == 1
      applicationResp.applications[0].publicId == "aaaaaaa-testidegrandfathering"
      applicationResp.applications[0].id == "e06a119c75d04d97b8d8c11b62719752"
      applicationResp.applications[0].organizationId == "2d0dfb87b67b43a3b4271e462bae9eca"
      organizationsResp != null
      organizationsResp.organizations.size > 0
      organizationsResp.organizations.find { it.id == applicationResp.applications[0].organizationId }.name == "Automatically Created Apps"
  }

  @Requires({env.JIRA_IQ_ARE_LOCAL})
  def 'helper test to verify interaction with IQ Server - Lookup Organization Name for App Id'() {
    when:
      def resp = clientLive.lookupOrganizationName("aaaaaaa-testidegrandfathering" )

    then:
      resp == "Automatically Created Apps"
  }
}
