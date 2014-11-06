package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

class jiraRestService {

	static transactional = false
	def hBuilderService
	private String jrapi = '/rest/api/2/issue/' 
	
	def updatecustomField(String jirauser,String jirapass, String jiraserver,String jiraticket,String customfield,String jenresults) {
		String url=jrapi+jiraticket
		try {
			RESTClient http = hBuilderService.httpConn(jiraserver, jirauser, jirapass)
			http.request( PUT, JSON ) { req ->
				uri.path = url
				
				body = [ fields:[ customfield: "$jenresults" ] ]
				
				response.success = { resp ->
					log.debug "Process URL Success! ${resp.status}"
				}
				response.failure = { resp ->
					log.error "Process URL failed with status ${resp.status}"
				}
			}
		}
		catch (HttpResponseException e) {
			return "Failed error: $e.statusCode"
		}
	}
	
	
	/*
	 *
	def jiratest(String jirauser,String jirapass, String jiraserver,String jiraticket,String customfield,String jenresults) {
		BasicCredentials creds = new BasicCredentials("${jirauser}", "${jirapass}")
		JiraClient jira = new JiraClient("${jiraserver}", creds)
		try {
			Issue issue = jira.getIssue("${jiraticket}")
			Object cfvalue = issue.getField("${customfield}");
			issue.update().field("${customfield}", "${jenresults}").execute()
		} catch (JiraException ex) {
			log.error(ex.getMessage())
			if (ex.getCause() != null) {
				log.error(ex.getCause().getMessage())
			}
		}
	}
	*/

	
	
	/*
	 *
	 * curl -D- -u fred:fred -X PUT --data {see below} -H "Content-Type: application/json" http://kelpie9:8081/rest/api/2/issue/QA-31
	 * {
   "update": {
	  "comment": [
		 {
			"add": {
			   "body": "It is time to finish this task"
			}
		 }
	  ]
   }
}
	 */
	
	/*
	 * curl -D- -u fred:fred -X PUT --data {see below} -H "Content-Type: application/json" http://kelpie9:8081/rest/api/2/issue/QA-31

example input data
{
   "update": {
   "description": [
		 {
			"set": "JIRA should also come with a free pony"
		 }
	  ],
	  "comment": [
		 {
			"add": {
			   "body": "This request was originally written in French, which most of our developers can't read"
			}
		 }
	  ]
   }
}
*/
}

