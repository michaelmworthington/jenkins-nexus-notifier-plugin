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
  ComponentIdentifier componentIdentifier
  String policyId
  String policyName
  Integer policyThreatLevel
  String cvssReason

  //Jira Fields
  String ticketInternalId
  String ticketExternalId
  String ticketType
  String ticketStatus
  String ticketSummary

  //generated fields
  Set<String> findingFingerprints = new HashSet<String>()
  String detectDateString
  String componentFingerprintPrettyPrint
  String componentFingerprintKey
  String componentFingerprint
  String fingerprintPrettyPrint
  String fingerprintKey
  String fingerprint
  Double cvssScore
  String cveCode
  String severity

  static boolean assignableFrom(Object action) {
    return action.hasProperty('reportLink') && action.hasProperty('affectedComponentCount') &&
        action.hasProperty('criticalComponentCount') && action.hasProperty('severeComponentCount') &&
        action.hasProperty('moderateComponentCount')
  }

  static void buildFromIQ(Map<String, PolicyViolation> potentialComponentsMap,
                          Map<String, PolicyViolation> potentialFindingsMap,
                          Object component,
                          String pReportLink,
                          String iqAppExternalId,
                          String policyFilterPrefix)
  {
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(component.componentIdentifier)
    String componentName = componentIdentifier.prettyName

    String componentFingerprintPrettyPrint = "${componentName}"
    String componentFingerprintKey = "SONATYPEIQ-${iqAppExternalId}-${componentName}"
    String componentFingerprintHash = getFingerprintHash(componentFingerprintKey)

    PolicyViolation potentialComponentViolation = new PolicyViolation(reportLink: pReportLink,
                                                                      componentIdentifier: componentIdentifier,
                                                                      fingerprintPrettyPrint: componentFingerprintPrettyPrint,
                                                                      fingerprintKey: componentFingerprintKey,
                                                                      fingerprint: componentFingerprintHash)

    component.activeViolations?.each {
      if (policyFilterPrefix == null || it.policyName.startsWith(policyFilterPrefix))
      {
        String conditionReasonText = ""
        String fingerprintPrettyPrint = "${componentName} - ${it.policyName}"
        String fingerprintKey = "SONATYPEIQ-${iqAppExternalId}-${it.policyId}-${componentName}"
        String cveCode = ""
        Double cvssScore = 0
        String severity = ""

        //Add in CVE - multiple SECURITY-HIGH violations
        // this might be a shortcut, i don't think an active violation would have multiple conditions
        def cvssCondition = it.constraints[0].conditions.find { "SecurityVulnerabilitySeverity" == it.conditionType }
        def licenseCondition = it.constraints[0].conditions.find { "License" == it.conditionType || "License Threat Group" == it.conditionType }

        if(cvssCondition)
        {
          conditionReasonText = cvssCondition.conditionReason
          fingerprintPrettyPrint = "${componentName} - ${it.policyName} - ${conditionReasonText}"
          fingerprintKey = "SONATYPEIQ-${iqAppExternalId}-${it.policyId}-${componentName}-${conditionReasonText}"

          //Parse the CVSS Reason for CVE Code and CVSS Score
          String[] parts = conditionReasonText.split(' ')
          cvssScore = Double.parseDouble(parts[6][0..-2])
          cveCode = parts[3]
          severity = parseSecuritySeverity(it.policyName)
        }
        else if (licenseCondition)
        {
          conditionReasonText = licenseCondition.conditionReason
          fingerprintPrettyPrint = "${componentName} - ${it.policyName} - ${conditionReasonText}"
          fingerprintKey = "SONATYPEIQ-${iqAppExternalId}-${it.policyId}-${componentName}-${conditionReasonText}"
          //TODO: do i get the license name here?
        }

        String findingFingerprintHash = getFingerprintHash(fingerprintKey)

        PolicyViolation policyViolation = new PolicyViolation(reportLink: pReportLink,
                                                              componentIdentifier: componentIdentifier,
                                                              componentFingerprintPrettyPrint: componentFingerprintPrettyPrint,
                                                              componentFingerprintKey: componentFingerprintKey,
                                                              componentFingerprint: componentFingerprintHash,
                                                              policyId: it.policyId,
                                                              policyName: it.policyName,
                                                              policyThreatLevel: it.policyThreatLevel,
                                                              cvssReason: conditionReasonText,
                                                              cvssScore: cvssScore,
                                                              cveCode: cveCode,
                                                              severity: severity,
                                                              fingerprintPrettyPrint: fingerprintPrettyPrint,
                                                              fingerprintKey: fingerprintKey,
                                                              fingerprint: findingFingerprintHash)

        //add the component down here, so we don't add components when all the findings are filtered out
        // WARNING this is the key to making sure that the tasks+subtasks get closed properly
        potentialComponentsMap.put(componentFingerprintHash, potentialComponentViolation)
        potentialComponentViolation.addViolationToComponent(policyViolation)
        potentialFindingsMap.put(findingFingerprintHash, policyViolation)
      }
    }
  }

  static PolicyViolation buildFromJira(Object ticket, String violationIdCustomFieldId)
  {
    // WARNING WARNING WARNING
    // I am filtering these when querying to limit the data sent between jenkins and jira
    //you need to update the filter any fields are added to this list
    //as a catch all, there is an option on the step to remove that filter
    // WARNING WARNING WARNING
    String ticketInternalId = ticket.id
    String ticketExternalId = ticket.key
    String ticketType = ticket.fields.issuetype.name
    String summary = ticket.fields.summary
    String status = ticket.fields.status.name
    String fingerprint = safeCustomFieldLookup(ticket, violationIdCustomFieldId)

    return new PolicyViolation(ticketStatus: status,
                               ticketType: ticketType,
                               ticketExternalId: ticketExternalId,
                               ticketInternalId: ticketInternalId,
                               ticketSummary: summary,
                               fingerprint: fingerprint)
  }

  private static String safeCustomFieldLookup(ticket, String pCustomFieldId)
  {
    if(pCustomFieldId)
    {
      ticket.fields.get(pCustomFieldId)
    }
    else
    {
      return null
    }
  }

  private void addViolationToComponent(PolicyViolation policyViolation)
  {
    if (policyThreatLevel < policyViolation.policyThreatLevel)
    {
      policyThreatLevel = policyViolation.policyThreatLevel
      policyName = policyViolation.policyName
      policyId = policyViolation.policyId
    }

    if (cvssScore < policyViolation.cvssScore)
    {
      cvssScore = policyViolation.cvssScore
      severity = policyViolation.severity
      cveCode = policyViolation.cveCode
      cvssReason = policyViolation.cvssReason
    }

    findingFingerprints.add(policyViolation.fingerprint)
  }

  private static String parseSecuritySeverity(String pPolicyName)
  {
    pPolicyName.replaceFirst("Security-", "")
  }

  private static String getFingerprintHash(final String fingerprintKey)
  {
    MessageDigest.getInstance("SHA-256").digest(fingerprintKey.bytes).encodeHex().toString()
  }
}
