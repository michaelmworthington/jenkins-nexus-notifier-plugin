package org.sonatype.nexus.ci.jenkins.util

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovyx.net.http.HttpResponseException
import org.sonatype.nexus.ci.jenkins.http.SonatypeHTTPBuilder

abstract class AbstractToolClient
{
  static String USER_AGENT = 'nexus-jenkins-notifier'

  SonatypeHTTPBuilder http
  String serverUrl
  String username
  String password
  PrintStream logger
  boolean verboseLogging

  AbstractToolClient(String serverUrl,
                     String username,
                     String password,
                     PrintStream logger,
                     final boolean verboseLogging = false)
  {
    this.http = new SonatypeHTTPBuilder()
    this.serverUrl = serverUrl
    this.username = username
    this.password = password
    this.logger = logger
    this.verboseLogging = verboseLogging

    //this.http.parser.'text/html' = this.http.parser.'text/plain'
    //http.parser.'application/json' = http.parser.'text/plain'

    //Jira returns HTML for 401/403 rather than json. This causes parsing errors
    // and, the error messages get garbled up in 2.46.3
    // 1. try to provide a better error message, maybe a test button in the config
    // 2. handle the parsing
    //
    //https://github.com/jgritman/httpbuilder/wiki/Content-Types
    //https://stackoverflow.com/questions/8015391/groovy-httpbuilder-and-jackson
    //https://www.jfrog.com/jira/browse/HAP-935
    //https://groups.google.com/forum/#!topic/jenkinsci-users/n5mKk4bubhc
    //https://issues.jenkins-ci.org/browse/JENKINS-39346
    //https://issues.jenkins-ci.org/browse/JENKINS-38445



    if(logger)
    {
      /*
       * Jira sends the error message in the body. Let's print that out in addition to the stack trace
       *   ref: https://stackoverflow.com/questions/19966548/groovy-httpbuilder-get-body-of-failed-response
       */
      this.http.handler.failure = { resp, reader ->
        logger.println("Error Response Code: ${resp.status} with message: ${reader}")
        //[response:resp, reader:reader]
        if(verboseLogging)
        {
          logger.println("######################################")
          def req = resp?.context?.delegate?.map?.get("http.request")?.original
          logger.println("REQUEST:  " + req)
          if (req.hasProperty('entity'))
          {
            logger.println(JsonOutput.prettyPrint(resp?.context?.delegate?.map?.get("http.request")?.original?.entity?.content?.getText()))
          }
          logger.println("######################################")
        }

        throw new HttpResponseException(resp)
      }

      if(verboseLogging)
      {
        this.http.handler.success = { resp, parsedData ->
          logger.println("######################################")
          def req = resp?.context?.delegate?.map?.get("http.request")?.original
          logger.println("REQUEST:  " + req)
          if (req.hasProperty('entity'))
          {
            logger.println(JsonOutput.prettyPrint(resp?.context?.delegate?.map?.get("http.request")?.original?.entity?.content?.getText()))
          }
          logger.println("######################################")
          logger.println("RESPONSE: " + resp?.responseBase?.statusline)
          logger.println(new JsonBuilder(parsedData).toPrettyString())
          logger.println("######################################")

          return this.http.defaultSuccessHandler(resp, parsedData)
        }
      }
    }
  }

  static Map getRequestHeaders(username, password)
  {
    return [
            'User-Agent' : USER_AGENT,
            Authorization: 'Basic ' + ("${username}:${password}").bytes.encodeBase64()
    ]
  }

  void verbosePrintLn(String pMessage)
  {
    if (verboseLogging)
    {
      logger.println(pMessage)
    }
  }
}
