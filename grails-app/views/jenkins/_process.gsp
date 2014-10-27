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
.red {
	background: #FFB2B2;
}
.green {
	background: #CCEBCC;
}
.orange{
	background: #FFD6AD;
}
.blue {
	background:#B2E0FF;
}
.grey {
	background:#EEE;
}

</style>
	<body>
	<div style="clear:both;"></div>
	<br/><p></p>
	<h3>${jenschoice } : ${jensjob} on ${jenserver }</h3>
	<g:if test="${((!hideButtons) || (!hideButtons.toLowerCase().equals('yes')))}">	
		<div class="container">
			<g:if test="${((!hideTriggerButton) || (!hideTriggerButton.toLowerCase().equals('yes')))}">
				
				<a class="jbutton" onclick="javascript:newBuild${divId}('build');">Trigger a build</a> 
			</g:if>
			<g:if test="${((!hideDashBoardButton) || (!hideDashBoardButton.toLowerCase().equals('yes')))}">	
				<a class="jbutton" onclick="javascript:newBuild${divId}('dashboard');">Dashboard</a>
			</g:if>
		</div>
	</g:if>	
	
		<g:if test="${((!hideBuildTimer) || (!hideBuildTimer.toLowerCase().equals('yes')))}">	
		
		</g:if>

<br/>
<div class="BuildHistory">
<div id="BuildHistory${divId}" >
</div>
</div>

<pre id="messagesTextarea${divId}" class="logconsole-sm">
</pre>

<script>

function wrapIt(value) {
	return "'"+value+"'"
}

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
	webSocket${divId}.send(JSON.stringify({'cmd':'connect','jensuser':"${jensuser }",'jensconurl':"${jensconurl }",'hideBuildTimer':"${hideBuildTimer }",
	'jenspass':"${jenspass }",'jenserver':"${jenfullserver }",'jensurl':"${jensurl }",'jensbuildend':"${jensbuildend }",'jensprogressive': "${jensprogressive }", 'jensconlog':"${jensconlog }"}));
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
		
		if (jsonData.historytop!=null) {
			jsonData.historytop.forEach(function(entry) {
				$('#BuildHistoryTop${divId}').html(entry.bprogress);
				//updateBuilds(entry.bid)
				updateBuilds${divId}()
			});
		}	

		if (jsonData.historyQueue!=null) {
			var sb = [];
			sb.push('<ul>');
			jsonData.historyQueue.forEach(function(entry) {
				if (entry.bstatus.indexOf('queued')>-1) {
					cclass='grey'
					sb.push('\n<li class='+cclass+' >'+entry.jobid+' : <small>has been queued | <a onclick="javascript:cancelQueue${divId}('+wrapIt(entry.bid)+');">CANCEL</a></small>\n</li>');
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
						cclass='green'
						sb.push('\n<li class='+cclass+'><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n</li>');
						break;
					case 'failed':
						cclass='red'
						sb.push('\n<li class='+cclass+'><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n</li>');						
						break;
					case 'building':
						cclass='blue'
							sb.push('\n<li class='+cclass+'><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n');
							sb.push('\n<a onclick="javascript:stopBuild${divId}('+wrapIt(entry.bid)+');"><small>STOP</small></a>\n');
							sb.push('<br/><small><span id="BuildHistoryTop${divId}" class="redfont"></span></small>\n');
							sb.push('</li>');
							//setTimeout(function(){  
							    //updateBuilds(entry.bid)
							    updateBuilds${divId}();
							//},600);			
						break;
					case 'cancelled':
						cclass='orange'
							sb.push('\n<li class='+cclass+' ><a onclick="javascript:viewHistory${divId}('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n</li>');
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


function newBuild${divId}(choice) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': choice }));
	scrollToBottom${divId}();
}

function updateBuilds${divId}() {
	if (hidebuildTimer!="yes") { 
		webSocket${divId}.send(JSON.stringify({'cmd': 'histref', 'bid': '0' }));
	}
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
</script>



</body>
</html>