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
package org.sonatype.nexus.ci.jenkins.model

import java.security.MessageDigest

class PolicyViolation
{
  //todo: jira ticket and iq violation should not be in the same data model

  //IQ Fields
  String reportLink
  String componentName
  String policyId
  String policyName
  String policyThreatLevel
  String cvssReason

  //Jira Fields
  String ticketInternalId
  String ticketExternalId
  String ticketStatus
  String ticketSummary

  //generated fields
  String fingerprintPrettyPrint
  String fingerprintKey
  String fingerprint


  static boolean assignableFrom(Object action) {
    return action.hasProperty('reportLink') && action.hasProperty('affectedComponentCount') &&
        action.hasProperty('criticalComponentCount') && action.hasProperty('severeComponentCount') &&
        action.hasProperty('moderateComponentCount')
  }

  static Set<PolicyViolation> buildFromIQ(Object component, String pReportLink, String iqAppExternalId, String policyFilterPrefix) {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>()

    //TODO: this may need to be done specifically for each supported format
    //      but for now, according to https://www.midgetontoes.com/2016/03/11/properties-ordering-of-groovy-jsonslurper-parsing/
    //      the attributes of the json will be sorted - TODO: I may want to parse it anyway so it's easy to read. maybe i can dump the whole thing...
    String componentName = component.componentIdentifier.format
    component.componentIdentifier.coordinates.each {
      componentName += ":${it.value}"
    }

    component.activeViolations.each {
      if (policyFilterPrefix == null || it.policyName.startsWith(policyFilterPrefix))
      {
        //Add in CVE - multiple SECURITY-HIGH violations
        def cvssCondition = it.constraints[0].conditions.find { "SecurityVulnerabilitySeverity" == it.conditionType }
        String cvssReason = cvssCondition?.conditionReason

        //TODO: do i want to use the fingerprint, or just the key string?
        String fingerprintPrettyPrint = "${componentName} - ${it.policyName} - ${cvssReason}"
        String fingerprintKey = "SONATYPEIQ-${iqAppExternalId}-${it.policyId}-${componentName}-${cvssReason}"
        String fingerprint = MessageDigest.getInstance("SHA-256").digest(fingerprintKey.bytes).encodeHex().toString()

        returnValue.add(new PolicyViolation(reportLink: pReportLink,
                                            componentName: componentName,
                                            policyId: it.policyId,
                                            policyName: it.policyName,
                                            policyThreatLevel: it.policyThreatLevel,
                                            cvssReason: cvssReason,
                                            fingerprintPrettyPrint: fingerprintPrettyPrint,
                                            fingerprintKey: fingerprintKey,
                                            fingerprint: fingerprint)
        )
      }
    }

    return returnValue
  }

  static PolicyViolation buildFromJira(Object ticket, String violationIdCustomFieldId)
  {
    String ticketInternalId = ticket.id
    String ticketExternalId = ticket.key
    //String description = ticket.fields.description
    String summary = ticket.fields.summary
    String status = ticket.fields.status.name
    //String application = ticket.fields.get(applicationCustomFieldId)
    //String organization = ticket.fields.get(organizationCustomFieldId)
    String fingerprint = ticket.fields.get(violationIdCustomFieldId)

    return new PolicyViolation(ticketStatus: status,
                               ticketExternalId: ticketExternalId,
                               ticketInternalId: ticketInternalId,
                               ticketSummary: summary,
                               fingerprint: fingerprint)
  }
}
