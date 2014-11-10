package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import java.util.logging.Logger;

import grails.converters.JSON
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient

class JiraRestService {
	
	def grailsApplication
	
	static transactional = false
	
	private String jrapi = '/rest/api/2/issue/'

	
	
	// Updates a given custom field with the provided value
	def addCustomField(String jiraserver, String jirauser, String jirapass, ArrayList jiraticket, ArrayList changeMap, String customfield, String input1) {
		def myMap = []
		jiraticket.each { jt ->
			 
			String url=jrapi+jt
			String validurl = verifyUrl(jiraserver, jt, jirauser ?: '', jirapass ?: '')
			if (validurl.startsWith('Success')) {
				myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
				// OR: myMap=[ "update":[ "customfield_${customfield}":  [ [  "set":  "$input1"  ] ] ] ]
				updateJira(jiraserver, jirauser, jirapass, url, myMap)
			}
		}
	}
	
	// Returns value of given customField
	def viewCustomField(String jiraserver, String jirauser, String jirapass, String jiraticket, String customfield ) {
		def myMap = []
		String url=jrapi+jiraticket
		String validurl = verifyUrl(jiraserver, jiraticket, jirauser ?: '', jirapass ?: '')
		if (validurl.startsWith('Success')) {
			return viewTicketField(jiraserver, jirauser, jirapass, url,customfield)
		}	
	}
	
	//update is appending to custom field - it will return viewCustomField and if different to input put together and repost it 
	def updateCustomField(String jiraserver, String jirauser, String jirapass, ArrayList jiraticket, ArrayList changeMap,  String customfield, String input1) {
		def myMap = []
		jiraticket.each { jt ->
			String url=jrapi+jt
			String validurl = verifyUrl(jiraserver, jt, jirauser ?: '', jirapass ?: '')
			if (validurl.startsWith('Success')) {
				def current=viewCustomField(jiraserver,jirauser,jirapass,jt,customfield)
				if (input1 != current) {
					def today = new Date()
					input1=current+"\n----------------------${today}--------------------------\n"+input1
				}
				myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
				updateJira(jiraserver, jirauser, jirapass, url, myMap)
			}else{
				log.error "${jiraserver}${url} returned ${validurl}"
			}
		}
	}

	// adds a comment a jira ticket
	def addComment(String jiraserver, String jirauser, String jirapass, ArrayList jiraticket, ArrayList changeMap, String input1) {
		def myMap = []
		jiraticket.each { jt ->
			String url=jrapi+jt
			String validurl = verifyUrl(jiraserver, jt, jirauser ?: '', jirapass ?: '')
			if (validurl.startsWith('Success')) {
				myMap = [ "update":[ "comment":[ [ "add": [ "body" : "$input1" ] ] ] ]  ]
				updateJira(jiraserver, jirauser, jirapass, url, myMap)
			}
		}		
	}

	// updates description and adds a comment to a jira ticket
	def updateDescAddComm(String jiraserver, String jirauser,String jirapass, ArrayList jiraticket, ArrayList changeMap, String input1,String input2) {
		def myMap = []
		jiraticket.each { jt ->
			String url=jrapi+jt
			String validurl = verifyUrl(jiraserver, jt, jirauser ?: '', jirapass ?: '')
			if (validurl.startsWith('Success')) {
				myMap = [ "update":[ "description":[ [ "set": "$input1"]], "comment":[ [ "add" :  [ "body": "${input2}"] ] ] ] ]
				updateJira(jiraserver, jirauser, jirapass, url, myMap)
			}	
		}	
	}
		
	// Updates description only
	def updateDesc(String jiraserver, String jirauser, String jirapass, ArrayList jiraticket, ArrayList changeMap, String input1) {
		def myMap = []
		jiraticket.each { jt ->
			String url=jrapi+jt
			String validurl = verifyUrl(jiraserver, jt, jirauser ?: '', jirapass ?: '')
			if (validurl.startsWith('Success')) {
				myMap = [ "update":[ "description":[ [ "set": "$input1"]] ] ] 
				updateJira(jiraserver, jirauser, jirapass, url, myMap)
			}
		}	
	}
	
	// HTTPBuilder GET request - returns ticket info as JSON and parses fields for given customField ID
	def viewTicketField(String jiraserver, String jirauser, String jirapass, String url, String customField) {
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
					log.info "Process URL Success! ${resp.status}"
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
	
	
	// Simply ensures URL is a successful URL.
	String verifyUrl( String server, String uri, String user, String pass) {
		String result = 'Failed'
		HBuilder hBuilder=new HBuilder()
		RESTClient http
		
		def config = grailsApplication.config.jenkins.jira
		def aurl=config.AccessUri ?: '/browse/'
		
		try {
			try {
				http = hBuilder.httpConn(server+aurl+uri, user ?: '', pass ?: '')
			}
			catch (HttpResponseException e) {
				return "Failed error: $e.statusCode"
			}
			catch (e)  {
				return "Failed error: ${e}"
			}
			catch (Throwable e) {
				return "Failed error: $e"
			}
			http?.request(Method.GET) { req ->
				//http?.request(GET, HTML) { req ->
				//if (uri) {
				//	uri.path = uri
				//}
				response.success = { resp ->
					result = "Success"
				}
				response.failure = { resp ->
					result = "Failed error: ${resp.statusLine.statusCode}"
				}
			}
		}
		catch (HttpResponseException e) {
			result = "Failed error: $e.statusCode"
		}
		return result
	}

}

