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
package org.sonatype.nexus.ci.jenkins.config.JiraConfiguration

import org.sonatype.nexus.ci.jenkins.config.JiraConfiguration
import org.sonatype.nexus.ci.jenkins.config.Messages

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

def typedDescriptor = (JiraConfiguration.DescriptorImpl) descriptor

f.section(title: typedDescriptor.displayName) {
  f.entry(title: _(Messages.Configuration_JiraServerUrl()), field: 'jiraServerUrl') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _(Messages.Configuration_JiraCredentials()), field: 'jiraCredentialsId') {
    c.select(context: app, includeUser: false, expressionAllowed: false)
  }

  f.entry(title: _(Messages.Configuration_IQServerUrl()), field: 'iqServerUrl') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _(Messages.Configuration_IQCredentials()), field: 'iqCredentialsId') {
    c.select(context: app, includeUser: false, expressionAllowed: false)
  }
}
