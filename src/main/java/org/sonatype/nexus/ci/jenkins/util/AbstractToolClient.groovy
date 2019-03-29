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
