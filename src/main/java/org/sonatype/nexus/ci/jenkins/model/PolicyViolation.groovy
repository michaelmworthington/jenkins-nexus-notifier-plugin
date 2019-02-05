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
  //IQ Fields
  String reportLink
  String componentName
  String policyName

  //Jira Fields
  String ticketInternalId
  String ticketExternalId

  //generated fields
  String fingerprintKey
  String fingerprint


  static boolean assignableFrom(Object action) {
    return action.hasProperty('reportLink') && action.hasProperty('affectedComponentCount') &&
        action.hasProperty('criticalComponentCount') && action.hasProperty('severeComponentCount') &&
        action.hasProperty('moderateComponentCount')
  }

  static Set<PolicyViolation> buildFromIQ(Object component, String pReportLink) {
    Set<PolicyViolation> returnValue = new HashSet<PolicyViolation>();

    //potentialFindings.aaData[0].activeViolations[0].policyName

    //TODO: this may need to be done specifically for each supported format
    //      but for now, according to https://www.midgetontoes.com/2016/03/11/properties-ordering-of-groovy-jsonslurper-parsing/
    //      the attributes of the json will be sorted - TODO: I may want to parse it anyway so it's easy to read. maybe i can dump the whole thing...

    String componentName = component.componentIdentifier.format
    component.componentIdentifier.coordinates.each{
      componentName += ":${it.value}"
    }

    component.activeViolations.each {

      //TODO: Filter by Policy
      //TODO: Add in CVE - multiple SECURITY-HIGH violations
      String fingerprintKey = "${componentName}-${it.policyName}"

      String fingerprint = MessageDigest.getInstance("SHA-256").digest(fingerprintKey.bytes).encodeHex().toString()

      returnValue.add(new PolicyViolation(reportLink: pReportLink,
                                          componentName: componentName,
                                          policyName: it.policyName,
                                          fingerprintKey: fingerprintKey,
                                          fingerprint: fingerprint) //TODO: do i want to use the fingerprint, or just the key string?
      );
    }

    return returnValue;
  }

  static PolicyViolation buildFromJira(Object ticket) {
    String ticketInternalId = ticket.id
    String ticketExternalId = ticket.key
    String description = ticket.fields.description //todo: how to parse the description
    String summary = ticket.fields.summary
    String status = ticket.fields.status.name



    String fingerprintKey = "${ticketExternalId}"

    String fingerprint = MessageDigest.getInstance("SHA-256").digest(fingerprintKey.bytes).encodeHex().toString()

    return new PolicyViolation(
            reportLink: "foo", //todo
            componentName: "bar",
            policyName: "baz",
            ticketExternalId: ticketExternalId,
            ticketInternalId: ticketInternalId,
            fingerprintKey: fingerprintKey,
            fingerprint: fingerprint
    )
  }
}
