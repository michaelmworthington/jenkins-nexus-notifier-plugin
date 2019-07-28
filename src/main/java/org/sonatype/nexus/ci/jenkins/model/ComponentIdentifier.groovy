package org.sonatype.nexus.ci.jenkins.model

/**
 * https://help.sonatype.com/iqserver/automating/rest-apis/using-other-supported-formats-with-the-rest-api
 *
 *       {"format":"maven","coordinates":{"artifactId": "json", "classifier": "", "extension": "jar", "groupId": "org.json", "version": "20080701"}}
 *       {"format":"npm","coordinates":{"packageId":"macaddress","version":"0.2.8"}}
 *       {"format":"nuget","coordinates":{"packageId":"SPAuthN","version":"2.1.3"}}
 *       {"format":"pypi","coordinates":{"extension":"tar.gz","name":"Flask-Admin","qualifier":"","version":"1.5.1"}}
 *       {"format":"a-name","coordinates":{"name":"vizion","qualifier":"","version":"0.1.0"}}
 *       {"format":"rpm","coordinas": {"name": "AGReader","version": "1.2-6.el6","architecture": "ppc64"}}
 *       {"format":"gem","coordinas": {"name": "rails","version": "5.","platform":""}}
 *
 *
 */
class ComponentIdentifier
{
  String format //todo: make an enum?
  String group
  String artifact
  String version
  String classifier
  String extension
  def coordinates
  String prettyName

  //TODO: GO LANG!!!!!
  ComponentIdentifier(Map pComponentIdentifierJson)
  {
    if (pComponentIdentifierJson)
    {
      format = pComponentIdentifierJson.format
      coordinates = pComponentIdentifierJson.coordinates
      switch (format)
      {
        case "maven":
          group = coordinates.groupId
          artifact = coordinates.artifactId
          version = coordinates.version
          classifier = coordinates.classifier
          extension = coordinates.extension
          break
        case "npm":
          //TODO: no group in the docs
          artifact = coordinates.packageId
          version = coordinates.version
          break
        case "nuget":
          //TODO: no group in the docs
          artifact = coordinates.packageId
          version = coordinates.version
          break
        case "pypi":
          artifact = coordinates.name
          version = coordinates.version
          extension = coordinates.extension
          classifier = coordinates.qualifier
          break
        case "gem":
          artifact = coordinates.name
          version = coordinates.version
          classifier = coordinates.platform
          break
        case "rpm":
          artifact = coordinates.name
          version = coordinates.version
          classifier = coordinates.architecture
          break
        case "a-name":
          artifact = coordinates.name
          version = coordinates.version
          classifier = coordinates.qualifier
          break
        default:
          break
      }
    }
    else
    {
      format = "unknown"
      //TODO: if null, probably a component unknown, is there any other info we can provide (i.e. the filename)?
    }

    prettyName = buildPrettyName()
  }

  private String buildPrettyName()
  {
    String returnValue = format

    switch (format)
    {
      case "maven":
      case "npm":
      case "nuget":
      case "pypi":
      case "gem":
      case "rpm":
      case "a-name":
        returnValue = [format, group, artifact, version, classifier, extension].findAll().join(":")
        break
      case "unknown":
        //todo - what else can we set for unknowns?
        coordinates?.each {
          returnValue += ":${it.value}"
        }
        break
      default:
        coordinates?.each {
          returnValue += ":${it.value}"
        }
        break
    }

    return returnValue
  }
}
