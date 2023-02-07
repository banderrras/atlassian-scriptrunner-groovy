package com.banderrras

import com.atlassian.jira.mail.Email
import com.atlassian.jira.mail.settings.MailSettings
import com.atlassian.mail.MailException
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.plugin.util.ContextClassLoaderSwitchingUtil
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.Method

public class Helper {
    
    //Jira: send email
    static String sendEmail(String emailAddr, String subject, String body, String cc, String bcc, Boolean importance) {
                

        // Stop emails being sent if the outgoing mail server gets disabled (useful if you start a script sending emails and need to stop it)
        def mailSettings = ComponentAccessor.getComponent(MailSettings)
        if (mailSettings?.send()?.disabled) {
            return 'Your outgoing mail server has been disabled'
        }

        def mailServer = ComponentAccessor.mailServerManager.defaultSMTPMailServer
        if (!mailServer) {
            return 'Failed to Send Mail. No SMTP Mail Server Defined'
        }

        def email = new Email(emailAddr)
        email.setMimeType('text/html')
        email.setSubject(subject)
        email.setBody(body)

        if(importance)
            email.addHeader("X-Priority", "1 (Highest)")

        if(!cc.empty)
            email.setCc(cc)
        if(!bcc.empty)
            email.setBcc(bcc)
        try {
            // This is needed to avoid the exception about IMAPProvider
            ContextClassLoaderSwitchingUtil.runInContext(SMTPMailServer.classLoader) {
                mailServer.send(email)
            }
            'Success'
        } catch (MailException e) {
            
            "Send mail failed with error: ${e.message}"
        }
    }

    //Confluence: Get page current version
    static int GetPageCurrentVersion(String baseUrl, String token, int pageId){
        def httpBuilder = new HTTPBuilder("$baseUrl/rest/api/content/$pageId?expand=version")

        int version = 0

        try{
            httpBuilder.request(Method.GET, ContentType.JSON) {
                headers."Authorization" = "Bearer $token"

                response.success = { response, json -> 
                    version = json.version.number
                }

                response.failure = { resp, data ->
                    if (data) {
                        "statusLine: $resp.status data: $data"
                    } else {
                        "statusLine: $resp.status"
                    }
                }    
            }   
        }
        catch(e){
            "Some other error occured: $e.message"
        }

        return version
    }

    //Confluence: Save content to page
    static def SendContentToConfluence(String baseUrl, String token, int pageId, String title, String content, int version){
        def httpBuilder = new HTTPBuilder("$baseUrl/rest/api/content/$pageId")

        try{
            httpBuilder.request(Method.PUT, ContentType.JSON) {
                headers."Authorization" = "Bearer $token"
                body = [
                            id : pageId,
                            type: "page",
                            title: title,
                            body: [
                                storage: [
                                    value: content,
                                    representation: "storage",
                                ]
                            ]  ,
                            version: [
                                number: version + 1,
                            ]
                        ]

                response.success = { response, json -> 
                    "Success"

                }

                response.failure = { resp, data ->
                    if (data) {
                        "statusLine: $resp.status data: $data"
                    } else {
                        "statusLine: $resp.status"
                    }
                }    
            }   
        }
        catch(e){
            "Some other error occured: $e.message"
        }
    }
}