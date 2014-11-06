package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

class jiraRestService {

	static transactional = false
	def hBuilderService
	private String jrapi = '/rest/api/2/issue/' 
	
	/*qtype being:
	 * 
	will update custom field / description and add a comment:
	    
	customfield  -- updates customg field input1 required only
	comment  -- adds a comment input1 required only
	description -- updates description requires input1 and input2 (description,body)
	
	
	*/
	def updateJira(String jirauser,String jirapass, String jiraserver,String jiraticket,String qtype,String customfield,String input1,String input2) {
		String url=jrapi+jiraticket
		try {
			RESTClient http = hBuilderService.httpConn(jiraserver, jirauser, jirapass)
			http.request( PUT, JSON ) { req ->
				uri.path = url
				
				if (qtype == "customfield") {
					body = [ fields:[ customfield: "$input1" ] ]
					
				} else if (qtype == "comment") {
					body = [ update:[ comment:[ add: [body : "$input1"] ] ] ]
					
				} else if (qtype == "description") {
					body = [ update:[ description:[ set: "$input1"], comment: [ body: "${input2}"] ] ]
				}
				
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
 	{"update": { "comment": [ {"add": {  "body": "It is time to finish this task"	} } ] }}
	{ "update": {  "description": [	 {	"set": "JIRA should also come with a free pony" } ], "comment": [{			"add": {
   "body": "This request was originally written in French, which most of our developers can't read"
	} } ] }}
	*/
}

