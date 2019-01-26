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

import groovy.json.JsonOutput
import org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder
import spock.lang.Ignore
import spock.lang.Specification

import java.security.MessageDigest

import static org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult.BuildStatus.FAIL
import static org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult.BuildStatus.PASS

class JiraClientTest
    extends Specification
{
  def http
  def client

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new JiraClient('http://localhost:8080', 'admin', 'admin123')
    client.http = http
  }

  def 'put card has correct url'() {
    def url

    when:
      client.putCard("DP", "", 1, 2, 3, 4, "http://example.com")

    then:
      1 * http.put(_, _, _) >> { args -> url = args[0]}

    and:
      url == 'http://localhost:7990/rest/insights/1.0/projects/int/repos/repo/commits/abcdefg/reports/sonatype-nexus-iq'
  }

  //@Ignore
  def 'helper test to verify interaction with Bitbucket Server - Get All Tickets'() {
    setup:
      def client = new JiraClient('http://localhost:59454', 'admin', 'admin123')
      def resp = client.lookupJiraTickets("DP")

    expect:
      resp != null
      resp.issues.size > 0
      resp.issues[0].key != null
  }

  //@Ignore
  def 'helper test to verify interaction with Bitbucket Server - Create Ticket'() {
    setup:
    def client = new JiraClient('http://localhost:59454', 'admin', 'admin123')
    def resp = client.createIssue("DP",
                                  "Sonatype IQ Server SECURITY-HIGH Policy Violation",
                                  "CVE-2019-1234",
                                  "SonatypeIQ:IQServerAppId:scanIQ",
                                  org.sonatype.nexus.ci.jenkins.bitbucket.PolicyEvaluationResult.BuildStatus.PASS,
                                  "SONATYPEIQ-APPID-COMPONENTID-SVCODE")

    expect:
    resp != null
    resp.key != null
  }

  //@Ignore
  def 'helper test to verify interaction with Bitbucket Server - Close Ticket'() {
    setup:
    def client = new JiraClient('http://localhost:59454', 'admin', 'admin123')
    def resp = client.closeTicket("10209")

    expect:
    resp == null
  }
}
