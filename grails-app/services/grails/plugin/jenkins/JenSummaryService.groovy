package grails.plugin.jenkins

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient

class JenSummaryService  extends JenJirConfService{

	static transactional = false

	def jenService

	def jiraRestService

	private ArrayList jiraTicket = []

	private Map createdFiles = [:]

	private String jensApi = '/api/json'
	private String consoleText = '/consoleText'
	private String changes = '/changes'

	RESTClient jenSummary(String jenserver, String jenuser,String jenpass,String bid,String sendSummary) {
		try {
			HBuilder hBuilder=new HBuilder()
			RESTClient http = hBuilder.httpConn(jenserver, jenuser ?: '', jenpass ?: '',httpConnTimeOut,httpSockTimeOut)
			jenSummary(http,jenserver,bid,sendSummary)
		}catch (HttpResponseException e) {
			log.error "Failed error: $e.statusCode"
		}

	}


	Map generatedFiles(RESTClient http, String jenserver, String bid) {
		jenSummary(http,jenserver,bid,'none')
		return createdFiles
	}


	String jenSummary(RESTClient http,String jenserver, String bid,String sendSummary) {
		String output=''
		createdFiles = [:]

		if (bid) {
			//String url2 = jenserver+"/"+bid+consoleText
			String cgurl=bid+changes

			boolean sendChanges = isConfigEnabled(config?.sendChanges.toString())
			boolean sendApi = isConfigEnabled(config?.sendApi.toString())
			boolean sendParseConsole = isConfigEnabled(config?.sendParseConsole.toString())

			String changes,jobstat,jobconsole=''

			if (sendApi) {
				jobstat=jobStatus(http,bid)
			}

			if (sendParseConsole) {
				jobconsole=definedParseJobConsole(http,bid)
			}

			if (sendChanges) {
				changes=parseChanges(http,cgurl)
			}

			StringBuilder sb=new StringBuilder()
			for (int a=0; a < 30; a++) {
				sb.append('-')
			}
			String sbb="\n"+sb.toString()+"\n"

			if (jobstat) {
				jobstat=jobstat+sbb
			}
			if (jobconsole) {
				jobconsole=jobconsole+sbb
			}
			output=jobstat+jobconsole+changes

			String sendtoJira = config.sendtoJira
			if ((sendtoJira && sendtoJira.toLowerCase().equals('yes'))&&((sendSummary) && (!sendSummary.equals('none')))) {
				String jiraSendType = sendSummary ?: config.jiraSendType
				String jiracustomField = config.customField
				// different Jira calls
				if (jiraSendType && jiraSendType.toLowerCase().equals('comment') && jiraTicket) {
					jiraRestService.addComment(jiraTicket,output)
				}else if (jiraSendType && jiraSendType.toLowerCase().equals('customfield') && jiraTicket && jiracustomField) {
					jiraRestService.addCustomField(jiraTicket,jiracustomField,output)
				}else if (jiraSendType && jiraSendType.toLowerCase().equals('updatecustomfield') && jiraTicket && jiracustomField) {
					jiraRestService.updateCustomField(jiraTicket,jiracustomField,output)
				}else if (jiraSendType && jiraSendType.toLowerCase().equals('description') && jiraTicket && jiracustomField) {
					jiraRestService.updateDesc(jiraTicket,output)
				}else if (jiraSendType && jiraSendType.toLowerCase().equals('comdesc') && jiraTicket && jiracustomField) {
					jiraRestService.updateDescAddComm(jiraTicket,output,output)
				}
			}
		}

		return output

	}


	String jobStatus(RESTClient http, String uri) {
		StringBuilder sb=new StringBuilder()
		if (!uri.startsWith('/')) {
			uri='/'+uri
		}

		boolean sendChangeSet = isConfigEnabled(config?.sendChangeSet.toString())
		boolean sendCulprits = isConfigEnabled(config?.sendCulprits.toString())
		boolean sendFdn = isConfigEnabled(config?.sendFdn.toString())
		boolean sendBuildId = isConfigEnabled(config?.sendBuildId.toString())
		boolean sendBuildUser = isConfigEnabled(config?.sendBuildUser.toString())


		def changes=parseApiJson(http, uri + jensApi)

		if (changes) {
			String result=changes?.result
			String buildId=changes?.number
			String bdate=changes?.id
			String fdn=changes?.fullDisplayName

			String changeSet=changes?.changeSet
			String culprits=changes?.culprits

			def userId,userName
			if (changes?.actions) {
				changes.actions.each { ca->
					ca.causes.each { cb->
						userId=cb?.userId
						userName=cb?.userName
					}
				}
			}

			if (fdn&& sendFdn ) {
				sb.append("$fdn $bdate\n")
			}

			if (buildId && sendBuildId) {
				sb.append("Build_ID: $buildId\n")
				sb.append("Status: $result\n")
			}

			if (userId && sendBuildUser) {
				sb.append("By: $userId $userName\n")
			}

			if (changeSet && sendChangeSet) {
				sb.append("ChangeSet: $changeSet \n")
			}

			if (culprits && sendCulprits) {
				sb.append("Changes by: ${culprits}\n")
			}

		}
		sb.toString()
	}

	String parseChanges(RESTClient http, String url) {
		def col3=[]
		def col4=[]

		StringBuilder sb=new StringBuilder()
		HttpResponseDecorator html1 = http.get(path: "$url")
		def html = html1?.data

		def sbn = html."**".findAll {it.@id.toString().contains("main-panel")}
		boolean go=true

		sbn.each {
			if (it.toString().trim().replaceAll("(\\r\\n|\\n)", '')
			.replaceAll("\\s+", " ").contains('No changes')) {
				go=false
			}
		}

		if (go) {
			if (sbn) {
				sbn.OL.each { ol->
					ol.LI.eachWithIndex { it, index ->
						col3 += [nid: index, cid : parseTicket(it.text()),
							cinfo : parseTicketInfo(it.text())]
					}
				}
			}
			int dd=0
			def sbn1 = html."**".findAll {it.@class.toString().contains("changeset-message")}
			if (sbn1) {
				sbn1.each { mg->
					col4 += [ nid: "${dd}", cid : "${col3[dd]?.cid}",
						message: mg.toString().trim().replaceAll("(\\r\\n|\\n)", '')
						.replaceAll("\\s+", " ") ]
					dd++
				}
			}

			def changeMap=[]
			if (col3&&col4) {
				changeMap = (col3 + col4).groupBy { it.nid }.collect { it.value.collectEntries { it } }
			}else{
				changeMap=col3
			}

			if (changeMap) {
				changeMap.eachWithIndex { entry,index ->
					sb.append("\n\n${index}> ${entry.cid} : ${entry.cinfo ?: 'No title'}\n${entry.message ?: 'No Message'}\n")
				}
			}
		}
		sb.toString()
	}


	// Returns Summary results
	String definedParseJobConsole(RESTClient http, String bid) {
		String workspace,ftype,file=''

		boolean parseBuildingWorkSpace = isConfigEnabled(config?.parseBuildingWorkSpace.toString())
		boolean parseBuilding = isConfigEnabled(config?.parseBuilding.toString())
		boolean parseDoneCreating = isConfigEnabled(config?.parseDoneCreating.toString())
		boolean parseLastTrans = isConfigEnabled(config?.parseLastTrans.toString())

		def builds=[]

		try {
			http.request(Method.GET, ContentType.TEXT) { req ->
				uri.path = bid+consoleText
				requestContentType = TEXT

				response.success = { resp, reader ->
					reader.text.eachLine {line ->
						def results=(matchers(line,parseBuildingWorkSpace,parseBuilding,parseDoneCreating,parseLastTrans))
						if (results) {
							builds.add(results)
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace()
		}
		StringBuilder sb=new StringBuilder()
		if (builds) {
			builds.each { entry ->
				entry.each { k,v->
					//userSession.basicRemote.sendText("${k}: ${v}\n")
					sb.append("${k}: ${v}\n")
				}
			}
		}
		sb.toString()
	}


	private Map matchers (String line, boolean parseBuildingWorkSpace, boolean parseBuilding, boolean parseDoneCreating, boolean parseLastTrans) {
		def bitem=[:]
		def matcher
		if (parseBuildingWorkSpace) {
			matcher = (line =~ /Building (.*?)(?: .*)? workspace (.*)/)
			if (matcher.matches()){
				bitem.put('workspace', matcher[0][2])
			}
		}

		if (parseBuilding) {
			matcher = (line =~ /(.*?)(?: .*)?Building (.*):(.*)/)
			if (matcher.matches()){
				//bitem.put('ftype', matcher[0][2])
				bitem.put('file', matcher[0][3])
				createdFiles.put(matcher[0][2],matcher[0][3])
			}
		}

		if (parseDoneCreating) {
			matcher = (line =~ /(.*?)(?: .*)?Done creating (.*) (.*)/)
			if (matcher.matches()){
				bitem.put('ftype', matcher[0][2])
				bitem.put('file', matcher[0][3])
				createdFiles.put(matcher[0][2],matcher[0][3])
			}
		}

		// Last transaction ID - is a block that is returned in Jenkins
		// May not apply to all types of logs
		if (parseLastTrans) {
			matcher = (line =~ /(.*?)Last valid trans (.*)/)
			if (matcher.matches()){
				def tid=matcher[0][2]
				if (tid) {
					tid=tid.toString().substring(1,tid.toString().length()-1)
					def tid1=jenService.returnArrary(tid)
					tid1.each {csv->
						if (csv.toString() =~ /[A-z]+\=[A-z]+/) {
							def (k,v) = csv.split('=')
							if (k=="id") { k="Last_Transaction_ID: "}
							bitem.put(k, v)
						}
					}
				}
			}
		}
		return bitem
	}


	/*
	 * Parse Jenkins API url - grab all but only using a few json values
	 *  to calculate estimated duration of build
	 */
	def parseApiJson(RESTClient http, String uri) {
		http.get(path: uri) { resp, json -> return json  }
	}



	private String parseTicketInfo(String input) {
		def output=''

		if (input && input.contains(':')) {
			output=input.split(':')[1].trim()
		} else if (input && input =~ /[A-z0-9]+ \- [A-z0-9]+/) {
			def (k,v) = input.split(' - ')
			output=v.trim()
		}else{
			output=input
		}

		if (!output) {
			output="undefined"
		}
		return output
	}

	private String parseTicket(String input) {
		def output=''
		if (input.contains(':')) {
			output=input.split(':')[0].trim()
		} else if (input =~ /[A-z0-9]+ \- [A-z0-9]+/) {
			def (k,v) = input.split(' - ')
			output=k.trim()
		}

		if (output && output =~ /[A-z]+\-[0-9]+/)  {
			if (!jiraTicket.contains(output)) {
				jiraTicket.add(output)
			}
		}
		else{
			output="undefined"
		}

		return output
	}

}
