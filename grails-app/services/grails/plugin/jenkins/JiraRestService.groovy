package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.converters.JSON
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

class JiraRestService {

	static transactional = false
	HBuilder hBuilder=new HBuilder()
	
	private String jrapi = '/rest/api/2/issue/' 
	

	def addCustomField(String jiraserver, String jirauser,String jirapass,String jiraticket,String customfield, String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
		println "${input1}"
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
	
	def addComment(String jiraserver, String jirauser,String jirapass,String jiraticket,String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap = [ update:[ comment:'[ [  add: [body : "$input1"] ] ]' ]  ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
	
	def updateDetail(String jiraserver, String jirauser,String jirapass,String jiraticket,String customfield, String input1,String input2) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap = [ update:[ description:[ set: "$input1"], comment: [ body: "${input2}"] ] ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
	
	def updateJira(String jiraserver, String jirauser,String jirapass,String url, Map myMap) {
		try {
			RESTClient http = hBuilder.httpConn(jiraserver, jirauser, jirapass)
			http.request( PUT, ContentType.JSON ) { req ->
				uri.path = url
				
				body = (myMap as JSON).toString()
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

