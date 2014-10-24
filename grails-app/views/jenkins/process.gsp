<!DOCTYPE html>
<html>
<head>
	<g:if test="${!request.xhr }">
    	<meta name='layout' content="main"/>
    </g:if>
   <title>Grails Jenkins plugin processor</title>
</head>
<body>

<jenkins:connect
divId="someId"
jenserver="${jenserver }"
jensport="${jensport}"
jensuser="${jensuser}"
jenspass="${jenspass}"
jensjob="${jensjob}"
jensfolder="${jensfolder}"
jensport="${jensport}"
/>
</body>
</html>