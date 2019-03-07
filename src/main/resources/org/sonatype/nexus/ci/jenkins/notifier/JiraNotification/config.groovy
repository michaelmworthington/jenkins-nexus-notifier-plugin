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
package org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

import org.sonatype.nexus.ci.jenkins.notifier.JiraNotification

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)
def typedDescriptor = (JiraNotification.DescriptorImpl) descriptor

f.section(title: typedDescriptor.displayName) {
  f.entry(title: _('Send Jira Notification'), field: 'sendJiraNotification') {
    f.checkbox()
  }

  f.entry(title: _('Jira Project Key'), field: 'projectKey') {
    f.textbox(clazz: 'required')
  }

  f.entry(title: _('Create Individual Tickets'), field: 'shouldCreateIndividualTickets') {
    f.checkbox()
  }

  f.entry(title: _('Aggregate Tickets by Component'), field: 'shouldAggregateTicketsByComponent') {
    f.checkbox()
  }

  f.entry(title: _('Transition Jira Tickets'), field: 'shouldTransitionJiraTickets') {
    f.checkbox()
  }

  f.entry(title: _('Jira Transition Status'), field: 'jiraTransitionStatus') {
    f.textbox()
  }

  f.entry(title: _('Create Tickets only for Policies starting with'), field: 'policyFilterPrefix') {
    f.textbox()
  }

  f.advanced() {
    f.section(title: _('Advanced Options')) {
      f.entry(title: _('Use job specific credentials for Jira'), field: 'jobJiraCredentialsId') {
        c.select(context: app, includeUser: false, expressionAllowed: false)
      }

      f.entry(title: _('Use job specific credentials for IQ Server'), field: 'jobIQCredentialsId') {
        c.select(context: app, includeUser: false, expressionAllowed: false)
      }
    }
    f.section(title: _('Custom Field Options - Derived Fields')) {
      f.entry(title: _('Jira Custom Field for IQ Application Id'), field: 'applicationCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for IQ Organization Id'), field: 'organizationCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for IQ Scan Stage'), field: 'scanStageCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Violation Unique Id'), field: 'violationIdCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Violation Detection Date'), field: 'violationDetectDateCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Last Scan Date'), field: 'lastScanDateCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Severity'), field: 'severityCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for CVE Code'), field: 'cveCodeCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for CVSS'), field: 'cvssCustomFieldName') {
        f.textbox()
      }
    }
    f.section(title: _('Custom Field Options - Pass Through Fields')) { //todo: can i make this a dynamic key/value list
      f.entry(title: _('Jira Custom Field for Scan Type - Field Name'), field: 'scanTypeCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Scan Type - Value'), field: 'scanTypeCustomFieldValue') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Tool Name - Field Name'), field: 'toolNameCustomFieldName') {
        f.textbox()
      }

      f.entry(title: _('Jira Custom Field for Tool Name - Value'), field: 'toolNameCustomFieldValue') {
        f.textbox()
      }
    }
  }
}
