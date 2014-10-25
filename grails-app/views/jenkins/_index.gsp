
	<i>Where this would be server:port/job/your_job_name</i>
	<g:form method="post" action="process" controller="jenkins" >
	<g:textField name="jenserver" placeholder="Jenkins Hostname/Server IP"/>
	<g:textField name="jensport" placeholder="Jenkins port"/>
	<g:textField name="jensprefix" placeholder="prefix" value=""/>
	<g:textField name="jensfolder" placeholder="usually job" value="job"/>
	<g:textField name="jensjob" placeholder="Your Jenkins jobname"/>
		<g:textField name="jensuser" placeholder="Jenkins username"/>
			<g:passwordField name="jenspass" placeholder="Jenkins password"/>
	<g:select name="jenschoice" from="${['dashboard':'Dashboard','build':'Build']}" 
	 optionKey="key" optionValue="value" class="many-to-one"/>
	<g:submitButton name="submit" value="Connect to Jenkins" />
	</g:form>
	