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
import org.sonatype.nexus.ci.jenkins.jira.JiraClient
import spock.lang.Ignore
import spock.lang.Specification

class IQClientTest
    extends Specification
{
  //private static final String port = "60359" //for Charles Proxy
  private static final String port = "8060"

  def http
  def client

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new IQClient("http://localhost:${port}/iq", 'admin', 'admin123', System.out, true)
    client.http = http
  }

  def 'lookup scan report policy threats has correct url'() {
    def url

    when:
      client.lookupPolcyDetailsFromIQ("a22d44d0209b47358c8dd2532bb7afb3", "aaaaaaa-testidegrandfathering")

    then:
      1 * http.get(_, _) >> { args -> url = args[0]}

    and:
      url == "http://localhost:${port}/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json"
  }

  @Ignore
  def 'helper test to verify interaction with IQ Server - Get Report Violations'() {
    setup:
      def client = new IQClient("http://localhost:${port}/iq", 'admin', 'admin123', System.out, true)
      //make it a real client instead of a mock http
      def resp = client.lookupPolcyDetailsFromIQ("3d0fedc4857f44368e0b501a6b986048", "aaaaaaa-testidegrandfathering")

    expect:
      resp != null
      resp.aaData.size > 0
      resp.aaData[0].hash != null
      resp.aaData[0].activeViolations.size == 2
      resp.aaData[0].waivedViolations.size == 1
  }
}
