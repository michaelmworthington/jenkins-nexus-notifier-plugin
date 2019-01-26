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

class IQClient
{
  static String USER_AGENT = 'nexus-jenkins-notifier'

  static String LOGO_URL = 'http://cdn.sonatype.com/brand/logo/nexus-iq-64-no-background.png'

  static String INSIGHT_KEY = 'sonatype-nexus-iq'

  private static final String API_GET_POLICY_EVALUATION_JSON = "{{iqURL}}/rest/report/{{iqAppExternalId}}/{{iqReportInternalId}}/browseReport/policythreats.json"

  SonatypeHTTPBuilder http

  String serverUrl

  String username

  String password

  IQClient(String serverUrl, String username, String password) {
    this.http = new SonatypeHTTPBuilder()
    this.serverUrl = serverUrl
    this.username = username
    this.password = password
  }

  Set lookupPolcyDetailsFromIQ(String s)
  {
    new HashSet() //TODO: implement
  }
}