<!DOCTYPE html>
<html>
<head>
	<g:if test="${!request.xhr }">
    	<meta name='layout' content="main"/>
    </g:if>
   <title>Grails Jenkins plugin processor</title>
</head>
<body>

<jen:connect
divId="someId"
jenserver="${jenserver }"
jensport="${jensport}"
jensuser="${jensuser}"
jenspass="${jenspass}"
jensjob="${jensjob}"
jensprefix="${jensprefix}"
jensfolder="${jensfolder}"
jenschoice="${jenschoice}"
hideButtons="${hideButtons }"
hideTriggerButton="${hideTriggerButton }"
hideDashBoardButton="${hideDashBoardButton }"
/>
</body>
</html>