package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import grails.converters.JSON
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient

class JiraRestService {

	static transactional = false
	
	private String jrapi = '/rest/api/2/issue/'

	// Updates a given custom field with the provided value
	def addCustomField(String jiraserver, String jirauser,String jirapass,String jiraticket,String customfield, String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
		// OR: myMap=[ "update":[ "customfield_${customfield}":  [ [  "set":  "$input1"  ] ] ] ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
	
	// Returns value of given customField
	def viewCustomField(String jiraserver, String jirauser,String jirapass,String jiraticket,String customfield ) {
		def myMap = []
		String url=jrapi+jiraticket
		return viewTicketField(jiraserver, jirauser, jirapass, url,customfield)	
	}
	
	//update is appending to custom field - it will return viewCustomField and if different to input put together and repost it 
	def updateCustomField(String jiraserver, String jirauser,String jirapass,String jiraticket,String customfield, String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		def current=viewCustomField(jiraserver,jirauser,jirapass,jiraticket,customfield)
		if (input1 != current) {
			def today = new Date()
			input1=current+"\n----------------------${today}--------------------------\n"+input1
		}
		myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}

	// adds a comment a jira ticket
	def addComment(String jiraserver, String jirauser,String jirapass,String jiraticket,String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap = [ "update":[ "comment":[ [ "add": [ "body" : "$input1" ] ] ] ]  ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}

	// updates description and adds a comment to a jira ticket
	def updateDescAddComm(String jiraserver, String jirauser,String jirapass,String jiraticket, String input1,String input2) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap = [ "update":[ "description":[ [ "set": "$input1"]], "comment":[ [ "add" :  [ "body": "${input2}"] ] ] ] ]
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
		
	// Updates description only
	def updateDesc(String jiraserver, String jirauser,String jirapass,String jiraticket, String input1) {
		def myMap = []
		String url=jrapi+jiraticket
		myMap = [ "update":[ "description":[ [ "set": "$input1"]] ] ] 
		updateJira(jiraserver, jirauser, jirapass, url, myMap)
	}
	
	// HTTPBuilder GET request - returns ticket info as JSON and parses fields for given customField ID
	def viewTicketField(String jiraserver, String jirauser,String jirapass,String url,String customField) {
		def output
		try {
			HBuilder hBuilder=new HBuilder()
			RESTClient http = hBuilder.httpConn(jiraserver, jirauser, jirapass)
			http.request(Method.GET ) { req ->
				uri.path = url
				response.success = { resp, json ->
					log.info "Process URL Success! ${resp.status}  "
					def result=JSON.parse(json.toString())
					if (result.fields."customfield_${customField}") {
						output=result.fields."customfield_${customField}"
					}	
				}
				response.failure = { resp ->
					log.error "Process URL failed with status ${resp.status}"
				}
			}
		}
		catch (HttpResponseException e) {
			return "Failed error: $e.statusCode"
		}
		return output
	}
	
	// HTTPBuilder PUT option to post/update a given jira ticket above actions
	def updateJira(String jiraserver, String jirauser,String jirapass,String url, Map myMap) {
		try {
			HBuilder hBuilder=new HBuilder()
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
}

