package org.sonatype.nexus.ci.jenkins.model

class IQRawLicenseData
{
  def declaredLicenses
  def observedLicenses
  def overriddenLicenses
  def status

  IQRawLicenseData(Map pLicenseData)
  {
    if (pLicenseData)
    {
      declaredLicenses = pLicenseData.declaredLicenses
      observedLicenses = pLicenseData.observedLicenses
      overriddenLicenses = pLicenseData.overriddenLicenses
      status = pLicenseData.status
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
      //do it separate because the unique won't work with the new lines already joined
      declaredLicenses.each{
        nameArray += it.licenseName
      }
      observedLicenses.each{
        nameArray += it.licenseName
      }
    }

    return nameArray.unique().join("\n")
  }
}
