import java.lang.Integer
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.ContentType
import groovyx.net.http.Method
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import com.banderrras.Helper
import com.banderrras.SRProperties
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.context.IssueContext
import com.atlassian.jira.issue.context.IssueContextImpl
import com.atlassian.jira.issue.fields.config.manager.PrioritySchemeManager
import com.atlassian.jira.issue.RendererManager
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin

String ConvertWikiToHtml(String wiki){
   RendererManager rendererManager = ComponentAccessor.getComponentOfType(RendererManager.class);
   JiraRendererPlugin renderer = rendererManager.getRendererForType("atlassian-wiki-renderer");
   return renderer.render(wiki, null)
}

String CreateIssue (String projectKey, String issueTypeName, String summary, String description){
   def issueService = ComponentAccessor.issueService
   def constantsManager = ComponentAccessor.constantsManager
   def loggedInUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
   def prioritySchemeManager = ComponentAccessor.getComponent(PrioritySchemeManager)

   def project = ComponentAccessor.projectManager.getProjectObjByKey(projectKey)
   assert project : "Could not find project with key $projectKey"

   def issueType = constantsManager.allIssueTypeObjects.findByName(issueTypeName)
   assert issueType : "Could not find issue type with name $issueTypeName"

   // if we cannot find user with the specified key or this is null, then set as a  reporter the logged in user
   def reporter = loggedInUser

   // if we cannot find the priority with the given name or if this is null, then set the default priority
   def issueContext = new IssueContextImpl(project, issueType) as IssueContext
   def priorityId = prioritySchemeManager.getDefaultOption(issueContext)

   def issueInputParameters = issueService.newIssueInputParameters().with {
      setProjectId(project.id)
      setIssueTypeId(issueType.id)
      setReporterId(reporter.name)
      setSummary(summary)
      setDescription(description)
      setPriorityId(priorityId)
   }

   def validationResult = issueService.validateCreate(loggedInUser, issueInputParameters)
   assert validationResult.valid : validationResult.errorCollection

   def result = issueService.create(loggedInUser, validationResult)
   assert result.valid : result.errorCollection

   return result.issue.getKey()
}


//Get plugin expiration date
Date getPluginExpirationDate (String pluginKey, String baseurl, String plugins_rest, String token) {

   def httpBuilder = new HTTPBuilder("$baseurl/$plugins_rest/$pluginKey-key/license")
   Date expirationDate


   try{
      httpBuilder.request(Method.GET, ContentType.ANY) {
         headers."Authorization" = "Bearer $token" 
         headers."Content-Type" = "application/vnd.atl.plugins.installed+json"
         headers."Content-Type" = "application/vnd.atl.plugins+json"


         response.success = { response, json ->
               if (json) {
                  
                  def jsonText = json.text 
                  
                  def parser = new JsonSlurper().setType(JsonParserType.LAX)
                  def jsonResp = parser.parseText(jsonText)

                  if(jsonResp.expiryDate)
                  {
                     expirationDate = new Date(jsonResp.expiryDate as Long)   
                  }
                  else if(jsonResp.maintenanceExpiryDate)
                  {
                     expirationDate = new Date(jsonResp.maintenanceExpiryDate as Long)
                  }
               }
         }
         response.failure = { resp, data ->
            if (data) {
               log.error([error: "Failed to make http request to $baseurl/$plugins_rest/$pluginKey-key/license", statusLine: resp.status, data: data.text])
            } else {
               log.error([error: "Failed to make http request to $baseurl/$plugins_rest/$pluginKey-key/license", statusLine: resp.status])
            }
         }
      }
   } catch(e){
      log.error([error: "Some other error occured: $e"])
   }

   return expirationDate
}

//Get nearly expired plugins list
String GetNearlyExpiredPlugins(String baseurl, String plugin_rest, String token, Integer days){
   String result = ""
   def httpBuilder = new HTTPBuilder(baseurl + plugin_rest)
   try{
      httpBuilder.request(Method.GET, ContentType.ANY) {
         headers."Authorization" = "Bearer $token" 
         headers."Content-Type" = "application/vnd.atl.plugins.plugin+jsons"
         headers."Content-Type" = "application/vnd.atl.plugins.plugin+json"

         response.success = { response, json ->
               if (json) {
                  
                  def jsonText = json.text 

                  //Setting the parser type to JsonParserLax
                  def parser = new JsonSlurper().setType(JsonParserType.LAX)
                  def jsonResp = parser.parseText(jsonText)

                  jsonResp.plugins.each {
                     if(it.usesLicensing && it.enabled)
                     {
                           Date expDate = getPluginExpirationDate(it.key, baseurl, plugin_rest, token)
                           Date now = new Date()

                           if(expDate)
                           {
                              if(expDate.minus(days).before(now))
                              {
                                    result += "$it.name Expiration Date: $expDate \r\n"
                              }
                           }
                     }
                  }
               }
         }
         response.failure = { resp, data ->
            if (data) {
               log.error([error: "Failed to make http request to $baseurl/$plugin_rest", statusLine: resp.status, data: data.text])
            } else {
               log.error([error: "Failed to make http request to $baseurl/$plugin_rest", statusLine: resp.status])
            }
         }
      }
   } catch(e){
      log.error([error: "Some other error occured: $e"])
   }

   return result
}

def plugins_rest = "/rest/plugins/1.0/"
final Integer days = 3
final String to = "test@test.com, test1@test.com"

final String summary = "Plugins will expire soon or have already expired"
final String issueProject = "TST"
final String issueType = "Support Request"


def props = SRProperties.instance.properties

def confluence_baseurl = "https://confluence.test.net"
def conf_authToken = props['confl_token'].toString()

def jira_baseurl = "https://jira.test.net"
def jira_token = props['jira_token'].toString()

def bitbucket_baseurl = "https://bitbucket.test.net"
def bitbucket_token = props['stash_token'].toString()

def output = ""

def conf_plugins = GetNearlyExpiredPlugins(confluence_baseurl, plugins_rest, conf_authToken, days)
if(!conf_plugins.empty)
   output += "*Confluence:*\r\n $conf_plugins"

def jira_plugins = GetNearlyExpiredPlugins(jira_baseurl, plugins_rest, jira_token, days)
if(!jira_plugins.empty)
   output += "*Jira:*\r\n $jira_plugins"

def stash_plugins = GetNearlyExpiredPlugins(bitbucket_baseurl, plugins_rest, bitbucket_token, days)
if(!stash_plugins.empty)
   output += "*Bitbucket:*\r\n $stash_plugins"

if(!output.empty){
   def key = CreateIssue(issueProject, issueType, summary, output)
   output += "\r\nIssue $key have been created."
   log.warn(key)
   return Helper.sendEmail(to, summary, ConvertWikiToHtml(output), "", "", true)
}
else
   return "No plugins"