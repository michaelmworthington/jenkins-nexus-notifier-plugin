package org.sonatype.nexus.ci.jenkins.model

class IQRawLicenseData
{
  def declaredLicenses
  def observedLicenses
  def overriddenLicenses

  IQRawLicenseData(Map pLicenseData)
  {
    if (pLicenseData)
    {
      declaredLicenses = pLicenseData.declaredLicenses
      observedLicenses = pLicenseData.observedLicenses
      overriddenLicenses = pLicenseData.overriddenLicenses
    }
  }

  String getDeclaredLicenseNames()
  {
    def nameArray = []
    declaredLicenses.each{
      nameArray += it.licenseName
    }

    return nameArray.join("\n")
  }

  String getObservedLicenseNames()
  {
    def nameArray = []
    observedLicenses.each{
      nameArray += it.licenseName
    }

    return nameArray.join("\n")
  }

  String getEffectiveLicenseNames()
  {
    def nameArray = []

    if(overriddenLicenses)
    {
      overriddenLicenses.each{
        nameArray += it.licenseName
      }
    }
    else
    {
      nameArray += getDeclaredLicenseNames()
      nameArray += getObservedLicenseNames()
    }

    return nameArray.unique().join("\n")
  }
}
