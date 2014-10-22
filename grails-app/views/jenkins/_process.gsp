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
.logconsole-sm {
width: 95%;
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
${jenfullserver }
<pre id="messagesTextarea${divId}" class="logconsole-lg">
</pre>
<script>

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

		webSocket${divId}.send(JSON.stringify({'cmd':'connect','jensuser':"${jensuser }",'jenspass':"${jenspass }",'jenserver':"${jenfullserver }",'jensurl':"${jensurl }",'jensbuildend':"${jensbuildend }",'jensprogressive': "${jensprogressive }", 'jensconlog':"${jensconlog }"}));
	
	  webSocket${divId}.send(JSON.stringify({'cmd': 'choose', 'jenschoice': "${jenschoice }" }));
  		
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
				//console.log ('Poll page is: '+jsonData.liveUrl);
				//pollPage(jsonData.nurl);
			}
		}else{
			$('#messagesTextarea${divId}').append(message.data);
			scrollToBottom();
		}
	}

	function scrollToBottom() {
		$('#messagesTextarea${divId}').scrollTop($('#messagesTextarea${divId}')[0].scrollHeight);
	}

	
     window.onbeforeunload = function() {
       	webSocket.send("DISCO:-");
       	webSocket.onclose = function() { }
       	webSocket.close();
     }




</script>

</body>
</html>