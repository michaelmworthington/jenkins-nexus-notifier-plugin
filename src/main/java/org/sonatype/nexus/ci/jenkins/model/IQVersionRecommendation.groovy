package org.sonatype.nexus.ci.jenkins.model

class IQVersionRecommendation
{
  def data
  String stage
  String nextNonFailingVersion
  String nextNoViolationsVersion

  IQVersionRecommendation(def pData, String pStage)
  {
    this.data = pData
    this.stage = pStage
    this.nextNonFailingVersion = data?.remediation?.versionChanges?.find({it.type == 'next-non-failing'})?.data?.component?.componentIdentifier?.coordinates?.version
    this.nextNoViolationsVersion = data?.remediation?.versionChanges?.find({it.type == 'next-no-violations'})?.data?.component?.componentIdentifier?.coordinates?.version
  }

  String getRecommendationText(String pCurrentVersion)
  {
    boolean currentIsNoViolations = pCurrentVersion == nextNoViolationsVersion
    boolean currentIsNonFailing = pCurrentVersion == nextNonFailingVersion
    boolean nonFailingIsNoViolations = nextNonFailingVersion == nextNoViolationsVersion

    String dateQualifier = "As of " + new Date().format("yyyy-MM-dd") + ":"

    if(currentIsNoViolations)
    {
      return "${dateQualifier}\n\t* Current version has no policy violations"
    }
    else if (!nextNonFailingVersion && !nextNoViolationsVersion)
    {
      return "${dateQualifier}\n\t* No recommended versions are available for the current component"
    }
    else if (nonFailingIsNoViolations)
    {
      return "${dateQualifier}\n\t* ${nextNoViolationsVersion}: Next version with no failing policy violations also has no violations"
    }
    else
    {
      def returnValue = []
      returnValue += dateQualifier

      if (currentIsNonFailing)
      {
        String text = "\t* Current version has no failing violations"
        if(stage)
        {
          text += " when evaluated at Nexus IQ Server scan stage: ${stage}"
        }
        returnValue += text
      }
      else if (nextNonFailingVersion)
      {
        String text = "\t* ${nextNonFailingVersion}: Next version with no failing policy"
        if(stage)
        {
          text += " when evaluated at Nexus IQ Server scan stage: ${stage}"
        }
        returnValue += text
      }

      if (nextNoViolationsVersion)
      {
        returnValue += "\t* ${nextNoViolationsVersion}: Next version with no policy violations"
      }
      else
      {
        returnValue += "\t* No recommended version clean of policy violations"
      }

      return returnValue.join("\n")
    }
  }
}
