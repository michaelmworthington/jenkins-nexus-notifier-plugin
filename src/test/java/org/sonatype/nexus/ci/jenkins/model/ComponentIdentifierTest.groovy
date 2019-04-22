package org.sonatype.nexus.ci.jenkins.model

import spock.lang.Specification

class ComponentIdentifierTest
        extends Specification
{
  def 'prettyName has correct format - gav only'()
  {
    setup:
    ComponentIdentifier componentIdentifier = new ComponentIdentifier([format     : "maven",
                                                                       coordinates: [groupId   : "org.apache.struts",
                                                                                     artifactId: "struts2-core",
                                                                                     version   : "1.2.3"]])

    expect:
    componentIdentifier.prettyName == "maven:org.apache.struts:struts2-core:1.2.3"
  }

  def 'prettyName has correct format - full gavce'()
  {
    setup:
    ComponentIdentifier componentIdentifier = new ComponentIdentifier([format     : "maven",
                                                                       coordinates: [groupId   : "org.apache.struts",
                                                                                     artifactId: "struts2-core",
                                                                                     version   : "1.2.3",
                                                                                     classifier: "linux",
                                                                                     extension : "war"]])

    expect:
    componentIdentifier.prettyName == "maven:org.apache.struts:struts2-core:1.2.3:linux:war"
  }
}
