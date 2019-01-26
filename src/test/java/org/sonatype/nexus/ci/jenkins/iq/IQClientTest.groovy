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
import spock.lang.Specification

class IQClientTest
    extends Specification
{
  def http
  def client

  def setup() {
    http = Mock(SonatypeHTTPBuilder)
    client = new IQClient('http://localhost:8060/iq', 'admin', 'admin123')
    client.http = http
  }

  def 'put card has correct url'() {
    def url

    when:
      client.lookupPolcyDetailsFromIQ("aaaaaaa-testidegrandfathering", "a22d44d0209b47358c8dd2532bb7afb3")

    then:
      1 * http.get(_, _) >> { args -> url = args[0]}

    and:
      url == 'http://localhost:8060/iq/rest/report/aaaaaaa-testidegrandfathering/a22d44d0209b47358c8dd2532bb7afb3/browseReport/policythreats.json'
  }

  //@Ignore
  def 'helper test to verify interaction with IQ Server - Get Report Violations'() {
    def url

    setup:
      def client = new IQClient('http://localhost:60359/iq', 'admin', 'admin123')
      //make it a real client instead of a mock http
      def resp = client.lookupPolcyDetailsFromIQ("aaaaaaa-testidegrandfathering", "a22d44d0209b47358c8dd2532bb7afb3")

    expect:
      resp != null
      resp.aaData.size > 0
      resp.aaData[0].hash != null
      resp.aaData[0].activeViolations.size == 2
      resp.aaData[0].waivedViolations.size == 1
  }
}
