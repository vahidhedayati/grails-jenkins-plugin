<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'jenkins.process.label', default: 'Processing')}" />
		<title><g:message code="jenkins.title.label" args="[entityName]" default="Jenkins Plugin" /></title>
	</head>
 <STYLE type="text/css">
 .jbutton {
	border: 1px solid rgba(0,0,0,0.2);
	box-sizing: content-box !important;
	color: #333;
	width: auto;
	display: inline-block;
    *zoom: 1;
    *display: inline;
	padding: 0.01em 0.02em;
	text-align: center;
	/*text-shadow: 0 0 5px rgba(0,0,0,0.2), 0 0 1px rgba(0,0,0,0.4);*/
	text-decoration: none;

	white-space: normal;
	outline: none; 
	/* Transition */
	-moz-transition: all 200ms ease 0ms !important;
	-o-transition: all 200ms ease 0ms !important;
	-webkit-transition: all 200ms ease 0ms !important;
	/* Background Color */
	background: none repeat scroll 0 0 rgba(255,255,255,0.04);
	/* Border Rounding */
	border-radius: 3px;
	-moz-border-radius: 3px;
	-webkit-border-radius: 3px;
	/* Background Clipping */
	-moz-background-clip: padding;
	-webkit-background-clip: padding;
	background-clip: padding-box;
	/* Shadow */
	box-shadow: 0 0 3px rgba(255,255,255,0.25) inset, 0 0 1px rgba(255,255,255,0.2), 0 10px 10px rgba(255,255,255,0.08) inset;
	-moz-box-shadow: 0 0 3px rgba(255,255,255,0.25) inset, 0 0 1px rgba(255,255,255,0.2), 0 10px 10px rgba(255,255,255,0.08) inset;
	-webkit-box-shadow: 0 0 3px rgba(255,255,255,0.25) inset, 0 0 1px rgba(255,255,255,0.2), 0 10x 10px rgba(255,255,255,0.08) inset;
	box-shadow: 0 0 3px rgba(255,255,255,0.25) inset, 0 0 1px rgba(255,255,255,0.2), 0 10px 10px rgba(255,255,255,0.08) inset;
}
.logconsole-lg {
width: 95%;
height: 500px;
border: 3px groove #ccc;
color: #FFF;
background: #000;
white-space: pre;
border-style: solid;
border-width: thin;
border-color: black;
font-size: 1em;
font-family: monospace;
display: inline-block;
*display:inline;
*zoom:1;
position: relative;
margin: 6px 6px 6px 6px;
padding: 12px 12px 12px 12px;
overflow:auto;
resize:both;
}	
.BuildHistoryTop {
display: block;
float:left;
color: #000;
background: #FFF;
font-size: 0.8em;
font-family: monospace;
position: relative;
overflow:auto;
resize:both;
}

.BuildHistory {
display: inline-block;
*display:inline;
*zoom:1;
float:right;
width: 25%;
height: 200px;
color: #000;
background: #FFF;
font-size: 0.8em;
font-family: monospace;
position: relative;
overflow:auto;
resize:both;
}

.logconsole-sm {

float:left;
width: 55%;
height: 200px;
border: 3px groove #ccc;
color: #FFF;
background: #000;
white-space: pre;
border-style: solid;
border-width: thin;
border-color: black;
font-size: 1em;
font-family: monospace;
display: inline-block;
*display:inline;
*zoom:1;
position: relative;
margin: 6px 6px 6px 6px;
padding: 12px 12px 12px 12px;
overflow:auto;
resize:both;
}

.redfont { 
 color: #0000FF;
 font-size: 0.8em;
}

.failed {
	background: #FFB2B2;
	border-style: solid;
border-width: thin;
border-color: black;
}

.queued {
	background: #CCEBCC;
	border-style: solid;
	border-width: thin;
	border-color: black;
}

.cancelled {
	background: #FFD6AD;
	border-style: solid;
	border-width: thin;
	border-color: black;
}

.building {
	background:#B2E0FF;
	border-style: solid;
	border-width: thin;
	border-color: black;
}


.queued {
	background:#EEE;
	border-style: solid;
	border-width: thin;
	border-color: black;
}
.passed {
	border-style: solid;
	border-width: thin;
	border-color: black;
}
.heading {
	font-size: 1.1em;
}

.jira {
 background:#FFEEDD;
}
.summary {
 background:#CCDDEE;
}
</style>
	<body>
	<div style="clear:both;"></div>
	<br/><p></p>
	<h3>${jenschoice } : ${jensjob} on ${jenserver }</h3>


	<g:if test="${((!hideButtons) || (!hideButtons.toLowerCase().equals('yes')))}">	
		<div class="container">
			<g:if test="${((!hideTriggerButton) || (!hideTriggerButton.toLowerCase().equals('yes')))}">
				<g:if test="${wsprocessurl && (autoSubmit.equals('yes')) && buildOnlyButton && (buildOnlyButton.equals('yes'))}">
					<a class="jbutton" onclick="javascript:justBuild${divId}();">Just Build</a> 
				</g:if>
				
				<a class="jbutton" onclick="javascript:newBuild${divId}('build');">Build
					<g:if test="${wsprocessurl && (autoSubmit.equals('yes'))}">
						&amp; ${wsprocessname}
					</g:if>
				</a> 
			</g:if>
			<g:if test="${((!hideDashBoardButton) || (!hideDashBoardButton.toLowerCase().equals('yes')))}">	
				<a class="jbutton" onclick="javascript:newBuild${divId}('dashboard');">Dashboard</a>
			</g:if>
		</div>
	</g:if>	
	
<br/>

<g:if test="${jensuser }">
<div class="jbutton">
	UserId: ${jensuser}
</div>
</g:if>

<g:if test="${dynamicName}">
<div class="jbutton">
${dynamicName} <g:select name="${dynamicName }" from="${dynamicValues }" onclick="setDynamicAction('${dynamicName}', this.value);"/>
</div>
</g:if>

<span id="jenkinsUser${divId}"></span>
<br/>
<div class="BuildHistory">

<div id="BuildHistory1${divId}" ></div>
<div id="BuildHistory${divId}" ></div>
</div>
<div id="return_${divId}"></div>
<div id="FeedBack${divId}"></div>
<pre id="messagesTextarea${divId}" class="logconsole-sm">
</pre>


<g:javascript>
var iDate${divId}='';
var jed${divId}=0;
function wrapIt(value) {
	return "'"+value+"'"
}

// Various websocket buttons configured via config.groovy as jenkins.name below
var summaryViewButtons="${summaryViewButtons }";
var summaryFileButton="${summaryFileButton }";
var summaryChangesButton="${summaryChangesButton }";

var divId="${divId}";

var jiraButtons="${jiraButtons }";
var jiraOverwriteButton="${jiraOverwriteButton }";
var jiraAppendButton="${jiraAppendButton }";
var jiraCommentButton="${jiraCommentButton }";
// end buttons

var hidebuildTimer="${hideBuildTimer}";

if (!window.WebSocket) {
	var msg = "Your browser does not have WebSocket support";
	$("#messagesTextarea${divId}").html(msg);
}


var webSocket${divId}=new WebSocket("ws://${wshostname}/${appname}/JenkinsEndPoint/${jenserver}/${jensjob}");
webSocket${divId}.onopen=function(message) {processOpen${divId}(message);};
webSocket${divId}.onclose=function(message) {processClose${divId}(message);};
webSocket${divId}.onerror=function(message) {processError${divId}(message);};
webSocket${divId}.onmessage=function(message) {processMessage${divId}(message);	};

function processOpen${divId}(message) {
	$('#messagesTextarea${divId}').append('Server Connect....\n');
	webSocket${divId}.send(JSON.stringify({'cmd':'connect','jensuser':"${jensuser }",'jensconurl':"${jensconurl }",
		'hideBuildTimer':"${hideBuildTimer }",'customParams':"${customParams}" , 
		'processurl':"${processurl}",'wsprocessurl':"${wsprocessurl}",'wsprocessname':"${wsprocessname}",
		'jenspass':"${jenspass }",'jenserver':"${jenfullserver }",'jensurl':"${jensurl }",
		'jensbuildend':"${jensbuildend }",'jensprogressive': "${jensprogressive }", 'jensconlog':"${jensconlog }"}));
	newBuild${divId}("${jenschoice }");
}

function processMessage${divId}(message) {
	var json;
	try {
		json = JSON.parse(message.data);
	} catch (exception) {
		json = null;
	}
	if(json) {
		var jsonData=JSON.parse(message.data);
		//$('#connectionCount').html(jsonData.connCount);
		
		if (jsonData.liveUrl!=null) {
			//console.log ('Poll page is: '+jsonData.liveUrl);
			//pollPage(jsonData.nurl);
		}
		
		if (jsonData.clearPage!=null) {
			$('#messagesTextarea${divId}').html("");
		}

		if (jsonData.feedback!=null) {
			var wsprocessurl = JSON.stringify(jsonData.feedback.wsprocessurl);
			var wsprocessname = JSON.stringify(jsonData.feedback.wsprocessname);
			var result = JSON.stringify(jsonData.feedback.result);
			var buildUrl = JSON.stringify(jsonData.feedback.buildUrl);
			var buildId = JSON.stringify(jsonData.feedback.buildId);
			var user = JSON.stringify(jsonData.feedback.user);
			var token = JSON.stringify(jsonData.feedback.token);
			var customParams = JSON.stringify(jsonData.feedback.customParams);
			var job = JSON.stringify(jsonData.feedback.job);
			var server = JSON.stringify(jsonData.feedback.server);
			var dynamicName = JSON.stringify(jsonData.feedback.dynamicName);
			var dynamicValue = JSON.stringify(jsonData.feedback.dynamicValue);
			
			var sb = [];
			var formId="submitForm${divId}";
			var remoteController="${remoteController }"
			var	remoteAction="${remoteAction }"
			var divId="${divId }"
			var baseapp="${meta(name:'app.name')}";
			var retId1="return_${divId}";
			function getApp() {
				return baseapp;
			}
			var submitTo="/${meta(name:'app.name')}/${remoteController }/${remoteAction }";
			var autoSubmit="${autoSubmit}";
			sb.push('<form method="post" id='+formId+' name='+formId+' action='+wsprocessurl+'>');
			sb.push('<input type="hidden" name="result" value='+result+'>');
			sb.push('<input type="hidden" name="buildUrl" value='+buildUrl+'>');
			sb.push('<input type="hidden" name="buildId" value='+buildId+'>');
			sb.push('<input type="hidden" name="user" value='+user+'>');
			sb.push('<input type="hidden" name="token" value='+token+'>');
			sb.push('<input type="hidden" name="customParams" value='+customParams+'>');
			sb.push('<input type="hidden" name="job" value='+job+'>');
			sb.push('<input type="hidden" name="server" value='+server+'>');
			sb.push('<input type="hidden" name="dynamicName" value='+dynamicName+'>');
			sb.push('<input type="hidden" name="dynamicValue" value='+dynamicValue+'>');
			
			sb.push('<input type="hidden" name="files" value='+jsonData.feedback.files+'>');
			sb.push('<input type="submit" name="doit" value='+wsprocessname+'>');
			
			sb.push('<input type="submit" name="doit" value='+wsprocessname+'>');
			
			sb.push('</form>');
			
			<g:if test="${formType.equals('remote')}">
				sb.push('<div id="'+retId1+'"></div>\n');
				// Remote Form functionality pushed out dynamically
				if (autoSubmit == "yes") { 
    				sb.push('<script>\n');
    				sb.push('var submitTo="/${meta(name:'app.name')}/${remoteController }/${remoteAction }";\n')
    				sb.push('var result='+result+'\n');
    				sb.push('var buildUrl='+buildUrl+'\n');
    				sb.push('var buildId='+buildId+'\n');
    				sb.push('var user='+user+'\n');
    				sb.push('var token='+token+'\n');
    				sb.push('var job='+job+'\n');
    				sb.push('var dynamicName='+dynamicName+'\n');
    				sb.push('var dynamicValue='+dynamicValue+'\n');
    				sb.push('var customParams='+customParams+'\n');
    				sb.push('var server='+server+'\n');
    				sb.push('var files='+jsonData.feedback.files+'\n');
    				sb.push('var retId='+retId1+'\n');
				 	sb.push('var xmlhttp;\n');
    	        	sb.push('if (window.XMLHttpRequest){\n');
    	            sb.push('xmlhttp = new XMLHttpRequest();\n');
    	        	sb.push('}\n');
    	        	sb.push('else{\n');
    	            sb.push('xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");\n');
    	        	sb.push('}\n');
    	        	sb.push('xmlhttp.onreadystatechange = function(){\n');
    	            sb.push('if (xmlhttp.readyState == 4 && xmlhttp.status == 200){\n');
    	            sb.push('document.getElementById("'+retId1+'").innerHTML = xmlhttp.responseText;\n');
    	            sb.push('}\n');
    	        	sb.push('}\n');
    	        	sb.push('xmlhttp.open("POST", submitTo, true);\n');
    	        	sb.push('xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");\n');
    	        	sb.push('xmlhttp.send("result="+result+"&dynamicName="+ dynamicName+"&dynamicValue="+ dynamicValue+"&buildUrl="+ buildUrl+"&buildId="+buildId+"&user="+user+"&token="+token+"&customParams="+customParams+"&job="+job+"&server="+server+"&files="+files);\n');
    	        	sb.push('<\/script>\n');	
				}else{
					sb.push('<script>\n');
					sb.push('$(\'#submitForm${divId}\').submit(function () {\n');
	    			sb.push('$.post('+wsprocessurl+', $(\'#submitForm${divId}\').serialize(), function (data, textStatus) {\n');
    	    		sb.push(' $(\'#return_${divId}\').append(data);\n');
  					sb.push('  });\n');
   					sb.push(' return false;\n');
 					sb.push('});\n');
					sb.push('<\/script>\n');
				}		
	    	</g:if>
	    	<g:else>
			if (autoSubmit == "yes") {
				sb.push('<script>\n');
				sb.push('document.getElementById("'+formId+'").submit();\n');
				sb.push('<\/script>\n');	
			}
			</g:else>	
			$('#FeedBack${divId}').html(sb.join(""));		
		}
		
		
		if (jsonData.historytop!=null) {
			jsonData.historytop.forEach(function(entry) {
				$('#BuildHistoryTop${divId}').html(entry.bprogress);
				updateBuilds${divId}()
			});
		}
		
		if (jsonData.buildNumber!=null) {
			$('#BuildNumber${divId}').html(jsonData.buildNumber);
		}
		
		if (jsonData.actions!=null) {
			jsonData.actions.forEach(function(entry) {
				if (entry.causes!=null) {
					entry.causes.forEach(function(en) {
						$('#jenkinsUser${divId}').html("<div class=\"jbutton\">Build user: "+en.userName+"</div>");
					});
				}
			});
		}		
		
		if (jsonData.estimatedDuration!=null) {
			if (jsonData.estimatedDuration=="N/A") {
				$('#BuildEstimation${divId}').html(jsonData.estimatedDuration);
			}else{
				jed${divId}=jsonData.estimatedDuration;
			}
		}
		
		if (jsonData.timestamp!=null) {
			var timeObject = new Date(jsonData.timestamp) 
			timeObject = new Date(timeObject .getTime() +jed${divId});
			cdtd${divId}();
			console.log('Estimated completion time: '+timeObject);
			iDate${divId}=timeObject;
			newBuild${divId}('dash');
		}

		
		if (jsonData.dash!=null) {
			dash${divId}();
		}	
		
		
		if (jsonData.historyQueue!=null) {
			var sb = [];
			sb.push('<ul>');
			jsonData.historyQueue.forEach(function(entry) {
				switch(entry.bstatus) {
					case 'queued':
						sb.push('\n<li class='+entry.bstatus+' ><span class="heading">'+entry.jobid+' : <small>has been queued |</span> <a onclick="javascript:cancelQueue${divId}('+wrapIt(entry.bid)+');">CANCEL</a></small>\n</li>');			
						break;
					case 'building':
						///sb.push('\n<li class='+entry.bstatus+'><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+'</small></a>\n');
						//sb.push('\n<a onclick="javascript:stopBuild${divId}('+wrapIt(entry.bid)+');"><small>STOP</small></a>\n');
						//sb.push('</li>');			
						break;	
					}	
			});		
			sb.push('</ul>')
			$('#BuildHistory1${divId}').html(sb.join(""));	
		}
		if (jsonData.history!=null) {
			//$('#BuildHistory${divId}').html("");
			var sb = [];
			sb.push('<ul>');
			jsonData.history.forEach(function(entry) {
				var ci=entry.bid.length;
				var cc=entry.bid.substring(0,ci - 1);	
				var crec=cc.substring(cc.lastIndexOf('/')+1,cc.length);
				var cclass=''
					switch(entry.bstatus) {
						case 'passed':
							sb.push('\n<li class='+entry.bstatus+'><span class="heading"><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : ');
							sb.push('<small>'+entry.bstatus+' '+entry.bdate+'</small></span></a>');
							if (summaryViewButtons == "yes") {
								sb.push(' <br><small><span class="summary">SUMMARY: | <a onclick="javascript:parseHistory${divId}('+wrapIt(entry.bid)+');">View</a> |');
								if (summaryFileButton == "yes") {
									sb.push(' <a onclick="javascript:parseFiles${divId}('+wrapIt(entry.bid)+');">Files</a> | ');
								}
								if (summaryChangesButton == "yes") {
									sb.push(' <a onclick="javascript:parseChanges${divId}('+wrapIt(entry.bid)+');">Changes</a> | ');
								}
								sb.push('</span>');
							}
							if (jiraButtons == "yes") {
								
								sb.push('<br><span class="jira"> JIRA: | ');
								if (jiraOverwriteButton == "yes") {
									sb.push('<a  title="Overwrite content of custom field" onclick="javascript:parseSendHistory${divId}('+wrapIt(entry.bid)+', \'customfield\');">customField</a> | ');
								}
								if (jiraAppendButton == "yes") {
									sb.push('<a title="Append to your custom field" onclick="javascript:parseSendHistory${divId}('+wrapIt(entry.bid)+', \'updatecustomfield\');">Append CF</a> | ');
								}
								if (jiraCommentButton == "yes") {
									sb.push('<a title="add as a comment to Jira" onclick="javascript:parseSendHistory${divId}('+wrapIt(entry.bid)+', \'comment\');">Comment</a>');							
								}
								sb.push(' | </span></small>');
							}
																
							sb.push('\n</li>');
							break;
						//case 'queued':
						//	sb.push('\n<li class='+entry.bstatus+' ><span class="heading">'+entry.jobid+' : <small>has been queued </span>| <a onclick="javascript:cancelQueue${divId}('+wrapIt(entry.bid)+');">CANCEL</a></small>\n</li>');			
						//	break;
						case 'failed':
							sb.push('\n<li class='+entry.bstatus+'><span class="heading"><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : ');
							sb.push('<small>'+entry.bstatus+' '+entry.bdate+'</small></span></a>');
							//sb.push(' | Other Act');
							sb.push('\n</li>');						
							break;
						case 'building':
							sb.push('\n<li class='+entry.bstatus+'><span class="heading"><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n');
							sb.push('\n<a onclick="javascript:stopBuild${divId}('+wrapIt(entry.bid)+');"></span><small>STOP</small></a>\n');
							sb.push('<br/><small><span id="BuildEstimation${divId}" class="redfont">');
							sb.push('<div class="jbutton">Estimated Time:');
							sb.push('<span id="hoursBox${divId}"></span>:');
							sb.push('<span id="minsBox${divId}"></span>:');
							sb.push('<span id="secsBox${divId}"></span>  Sec</div>');
							sb.push('</span></small>\n');
							sb.push('</li>');
							//setTimeout(function(){  
							    //updateBuilds(entry.bid)
							 //   updateBuilds${divId}();
							//},600);			
							break;
						case 'cancelled':
							sb.push('\n<li class='+entry.bstatus+' ><span class="heading"><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></span></a>\n</li>');
							break;			
					}			
			});
			sb.push('</ul>')
			$('#BuildHistory${divId}').html(sb.join(""));
		}	
	}else{
		$('#messagesTextarea${divId}').append(message.data);
		scrollToBottom${divId}();
	}
}

function setDynamicAction(name, value) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'dynamicAction', 'name': name, 'value': value}));
}

function parseHistory${divId}(bid) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'parseHistory', 'bid': bid }));
}

function parseFiles${divId}(bid) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'parseFiles', 'bid': bid }));
}

function parseChanges${divId}(bid) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'parseChanges', 'bid': bid }));
}

function parseSendHistory${divId}(bid,jiraSendType) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'parseSendHistory', 'bid': bid, 'jiraSendType' : jiraSendType }));
}	

function cdtd${divId}() {
	var future = new Date(iDate${divId});
	var now = new Date();
	var timeDiff = future.getTime() - now.getTime();
	var timer;
	var go=1;
	if (timeDiff <= 0) {
		newBuild${divId}('dash');
	    clearTimeout(timer);
	    go=0;
	}
	if (go==1) {
		var seconds = Math.floor(timeDiff / 1000);
		var minutes = Math.floor(seconds / 60);
		var hours = Math.floor(minutes / 60);
		var days = Math.floor(hours / 24);
		hours %= 24;
		    minutes %= 60;
		    seconds %= 60;
		//document.getElementById("daysBox${divId}").innerHTML = days;
		document.getElementById("hoursBox${divId}").innerHTML = hours;
		document.getElementById("minsBox${divId}").innerHTML = minutes;
		document.getElementById("secsBox${divId}").innerHTML = seconds;
		timer = setTimeout('cdtd${divId}()',1000);
	}
}


function  justBuild${divId}() {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': 'justBuild' }));
	scrollToBottom${divId}();
}

function newBuild${divId}(choice) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': choice }));
	scrollToBottom${divId}();
}
function dash${divId}() {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': 'dash' }));
	scrollToBottom${divId}();
}


function cancelQueue${divId}(jobid) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'cancelJob', 'bid': jobid }));
}

function stopBuild${divId}(bid) {
	//console.log('stop Build: '+bid);
	webSocket${divId}.send(JSON.stringify({'cmd': 'stopBuild', 'bid': bid }));
}

function viewHistory${divId}(bid) {
	//console.log('ViewHistory: '+bid);
	webSocket${divId}.send(JSON.stringify({'cmd': 'viewHistory', 'bid': bid }));
}

function scrollToBottom${divId}() {
	$('#messagesTextarea${divId}').scrollTop($('#messagesTextarea${divId}')[0].scrollHeight);
}

function processClose${divId}(message) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': 'disconnect' }));
	$('#messagesTextarea${divId}').append("Server Disconnected... \n");
}

function processError${divId}(message) {
	$('#messagesTextarea${divId}').append(" Error.... \n");
}
window.onbeforeunload = function() {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': 'disconnect' }));
	webSocket${divId}.onclose = function() { }
	webSocket${divId}.close();
}
</g:javascript>



</body>
</html>