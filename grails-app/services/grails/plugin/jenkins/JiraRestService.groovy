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

	/* TODO - let otherside be variable if not provided set defaults
	 private Map config = grailsApplication.config.jenkins
	 private String jiraServer = config.jiraServer
	 private String jiraUser = config.jiraUser
	 private String jiraPass = config.jiraPass
	 private String jiraSendType = config.jiraSendType
	 private String jiracustomField = config.customField
	 */

	static transactional = false

	private String jrapi = '/rest/api/2/issue/'


	// Updates a given custom field with the provided value
	def addCustomField(String jiraserver, String jirauser, String jirapass,
			ArrayList jiraticket,  String customfield, String input1) {

		jiraticket.each { jt ->

			String url=jrapi+jt

			def myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
			// OR: myMap=[ "update":[ "customfield_${customfield}":  [ [  "set":  "$input1"  ] ] ] ]

			updateJira(jiraserver, jirauser, jirapass, url, myMap)
		}

	}

	// Returns value of given customField
	def viewCustomField(String jiraserver, String jirauser, String jirapass,
			String jiraticket, String customfield ) {

		String url=jrapi+jiraticket

		return viewTicketField(jiraserver, jirauser, jirapass, url,customfield)

	}

	// update is appending to custom field - it will return
	// viewCustomField and if different to input put together and repost it
	def updateCustomField(String jiraserver, String jirauser, String jirapass,
			ArrayList jiraticket, String customfield, String input1) {

		jiraticket.each { jt ->
			def today = new Date()
			def current=viewCustomField(jiraserver,jirauser,jirapass,jt,customfield)

			if (current.toString().contains("[AUTOMATION]")) {
				def centry=current.toString().split(/\n\[AUTOMATION\](.*)\n/)
				boolean go=true
				centry.each { cent ->
					if (input1==cent) {
					//if (input1.toString().trim().equals(cent.toString().trim())) {
						go=false
						println "Summary information block found not adding again"
					}
				}
				if (go) {
					log.info "Adding New entry ${input1}"
					input1=current+"\n[AUTOMATION]----------------------${today}--------------------------\n"+input1
					def myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
					updateJira(jiraserver, jirauser, jirapass, jrapi+jt, myMap)
				}
			}else{

				if (input1!=current) {
				//if (input1.toString().trim().equals(current.toString().trim())) {
					input1=current+"\n[AUTOMATION]----------------------${today}--------------------------\n"+input1
					def myMap=[ "fields":[ "customfield_${customfield}": "$input1" ] ]
					updateJira(jiraserver, jirauser, jirapass, jrapi+jt, myMap)
				}else{
					log.info  "customField Append Entry matches existing entry not adding again"
				}
			}
		}
	}

	// adds a comment a jira ticket
	def addComment(String jiraserver, String jirauser, String jirapass,
			ArrayList jiraticket, String input1) {

		jiraticket.each { jt ->
			def myMap = [ "update":[ "comment":[ [ "add": [ "body" : "$input1" ] ] ] ]  ]
			updateJira(jiraserver, jirauser, jirapass, jrapi+jt, myMap)
		}

	}

	// updates description and adds a comment to a jira ticket
	def updateDescAddComm(String jiraserver, String jirauser,String jirapass,
			ArrayList jiraticket, String input1,String input2) {

		jiraticket.each { jt ->
			def myMap = [ "update":[ "description":[ [ "set": "$input1"]], "comment":[ [ "add" :  [ "body": "${input2}"] ] ] ] ]
			updateJira(jiraserver, jirauser, jirapass, jrapi+jt, myMap)
		}
	}

	// Updates description only
	def updateDesc(String jiraserver, String jirauser, String jirapass,
			ArrayList jiraticket, ArrayList changeMap, String input1) {

		jiraticket.each { jt ->
			def myMap = [ "update":[ "description":[ [ "set": "$input1"]] ] ]
			updateJira(jiraserver, jirauser, jirapass, jrapi+jt, myMap)
		}
	}


	// HTTPBuilder GET request - returns ticket info as JSON
	// and parses fields for given customField ID
	private String viewTicketField(String jiraserver, String jirauser,
			String jirapass, String url, String customField) {

		if (url.endsWith('undefined')) {
			return
		}

		String output

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
			log.error "Failed error: $e.statusCode"
		}
		return output
	}

	// HTTPBuilder PUT option to post/update a given jira ticket above actions
	def updateJira(String jiraserver, String jirauser,
			String jirapass,String url, Map myMap) {

		if (url.endsWith('undefined')) {
			return
		}

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
			log.error "Failed error: $e.statusCode"
		}
	}



}

