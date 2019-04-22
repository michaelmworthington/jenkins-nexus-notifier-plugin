package org.sonatype.nexus.ci.jenkins.model

/**
 *       {"format":"maven","coordinates":{"artifactId": "json", "classifier": "", "extension": "jar", "groupId": "org.json", "version": "20080701"}}
 *       {"format":"npm","coordinates":{"packageId":"macaddress","version":"0.2.8"}}
 *       {"format":"nuget","coordinates":{"packageId":"SPAuthN"}}
 *       {"format":"pypi","coordinates":{"extension":"tar.gz","name":"Flask-Admin","qualifier":"","version":"1.5.1"}}
 *       {"format":"a-name","coordinates":{"name":"vizion","qualifier":"","version":"0.1.0"}}
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
          //TODO: group
          artifact = coordinates.packageId
          version = coordinates.version
          break
        case "nuget":
          //TODO: group
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
          //todo
          artifact = coordinates.name
          break
        case "a-name":
          //TODO: what is the qualifier field
          artifact = coordinates.name
          version = coordinates.version
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
    //TODO: this may need to be done specifically for each supported format
    //      but for now, according to https://www.midgetontoes.com/2016/03/11/properties-ordering-of-groovy-jsonslurper-parsing/
    //      the attributes of the json will be sorted - TODO: I may want to parse it anyway so it's easy to read. maybe i can dump the whole thing...
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
        //todo
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
