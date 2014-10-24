<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'jenkins.process.label', default: 'Processing')}" />
		<title><g:message code="jenkins.title.label" args="[entityName]" default="Jenkins Plugin" /></title>
	</head>
 <STYLE type="text/css">
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
#BuildHistory {
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


	</style>
	<body>
<div class="button">
<a onclick="javascript:newBuild('build');">Trigger a build</a> |<a onclick="javascript:newBuild('dashboard');">Dashboard</a>
</div>
<br/>
<div id="BuildHistory"></div>
<pre id="messagesTextarea${divId}" class="logconsole-sm">
</pre>
<g:javascript>

function wrapIt(value) {
	return "'"+value+"'"
}

if (!window.WebSocket) {
	var msg = "Your browser does not have WebSocket support";
	$("#messagesTextarea${divId}").html(msg);
}


var webSocket${divId}=new WebSocket("ws://${wshostname}/${appname}/JenkinsEndPoint/${jenserver}/${jensjob}");
webSocket${divId}.onopen=function(message) {processOpen${divId}(message);};
// webSocket${divId}.onclose=function(message) {processClose${divId}(message);};
//webSocket${divId}.onerror=function(message) {processError${divId}(message);};
webSocket${divId}.onmessage=function(message) {processMessage${divId}(message);	};

function processOpen${divId}(message) {
	$('#messagesTextarea${divId}').append('Server Connect....\n');
	webSocket${divId}.send(JSON.stringify({'cmd':'connect','jensuser':"${jensuser }",'jensconurl':"${jensconurl }",
	'jenspass':"${jenspass }",'jenserver':"${jenfullserver }",'jensurl':"${jensurl }",'jensbuildend':"${jensbuildend }",'jensprogressive': "${jensprogressive }", 'jensconlog':"${jensconlog }"}));
	newBuild("${jenschoice }");
}

function newBuild(choice) {
	webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': choice }));
	scrollToBottom();
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
			console.log ('Poll page is: '+jsonData.liveUrl);
			//pollPage(jsonData.nurl);
		}
		if (jsonData.history!=null) {
			$('#BuildHistory').html("");
			var sb = [];
			sb.push('<ul>')
			jsonData.history.forEach(function(entry) {
				var ci=entry.bid.length;
				var cc=entry.bid.substring(0,ci - 1);	
				var crec=cc.substring(cc.lastIndexOf('/')+1,cc.length);
				var cclass=''
					if (entry.bstatus.indexOf('passed')) {
						cclass='green'
					}else if (entry.bstatus.indexOf('failed')) {
						cclass='red'
					}else if (entry.bstatus.indexOf('failed')) {
						cclass='blue'
					}	
				sb.push('\n<li ><a class='+cclass+'  onclick="javascript:viewHistory('+wrapIt(entry.bid)+');">'+entry.jobid+' : <small>'+entry.bstatus+' '+entry.bdate+'</small></a>\n</li>');


			});
			sb.push('</ul>')
			$('#BuildHistory').html(sb.join(""));
		}	
	}else{
		$('#messagesTextarea${divId}').append(message.data);
		scrollToBottom();
	}
}

function viewHistory(bid) {
	console.log('ViewHistory: '+bid);
	webSocket${divId}.send(JSON.stringify({'cmd': 'viewHistory', 'bid': bid }));
}

function scrollToBottom() {
	$('#messagesTextarea${divId}').scrollTop($('#messagesTextarea${divId}')[0].scrollHeight);
}


window.onbeforeunload = function() {
	webSocket.send("DISCO:-");
	webSocket.onclose = function() { }
	webSocket.close();
}
</g:javascript>



</body>
</html>