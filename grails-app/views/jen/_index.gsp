<g:javascript>
function toggleBlock(caller,called,calltext) {
	$(caller).click(function() {
		if($(called).is(":hidden")) {
 			$(caller).html('Hide '+calltext).fadeIn('slow');
    	}else{
        	$(caller).html('Show '+calltext).fadeIn('slow');	
        	
    	}
 		$(called).slideToggle("fast");
 	
  	});
  }
</g:javascript>
<style>
#vcontainer {
  display: table;
  height: 100%;
  width: 100%;
}
#hcontainer {
  display: table-cell;
  vertical-align: middle;
  text-align: center;
}
#content {
  display: inline-block;
  border: lightGray 1px solid;
  background-color: #FFF;
  text-align: left;
}
</style>

	  <div id="vcontainer"><div id="hcontainer">
    <div id="content">
   <h1>Method 1 Quick Connect to your Jenkins Job [Build / View Historical logs]</h1>
   
   <g:form method="post" action="process" controller="jenkins" >
   
   <div  class="fieldcontain">
	<div id="jensurl" style="display: none;"><div class="message" role="status">
		Jenkins full job url :  http://jenkins-server:port/job/jobname
	</div></div>
	<label>Jenkins Server</label>
	<g:textField name="jensurl" placeholder="Jenkins Full Job UL"/><a id="jensurlHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensurlHeader','#jensurl','');
	</g:javascript>
	</div>
	<div  class="fieldcontain">
	<div id="jensjob1" style="display: none;"><div class="message" role="status">
		Jenkins Job: Your jenkins job name
	</div></div>
	<label>Jenkins Job</label>
	<g:textField name="jensjob" /><a id="jensjob1Header"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensjob1Header','#jensjob1','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<div id="jensuser1" style="display: none;"><div class="message" role="status">
		username: If Jenkins can authenticate/requires it if not leave token blank
		
	</div></div>
	<label for="Username">Username</label>
		<g:textField name="jensuser" placeholder="Jenkins username"/><a id="jensuser1Header"><i>Help?</i></a>
		<g:javascript>
	toggleBlock('#jensuser1Header','#jensuser1','');
	</g:javascript>
	</div>
	
	
	<div  class="fieldcontain">
	<div id="jenschoice" style="display: none;"><div class="message" role="status">
		Your Choice dashboard displays build history or trigger a build
	</div></div>
	<label>Choice</label>
	<g:select name="jenschoice" from="${['dashboard':'Dashboard','build':'Build']}" 
	 optionKey="key" optionValue="value" class="many-to-one"/><a id="jenschoiceHeader"><i>Help?</i></a>
	 <g:javascript>
	toggleBlock('#jenschoiceHeader','#jenschoice','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<label></label>
	<g:submitButton name="submit" value="Connect to Jenkins" />
	</div>
	</g:form>
	

		
	<h1>Method 2 Connect to your Jenkins Job [Build / View Historical logs]</h1>
	<p></p>
	<g:form method="post" action="process" controller="jenkins" >
	
	<div  class="fieldcontain">
	<div id="jenserver" style="display: none;"><div class="message" role="status">
		Jenkins Server i.e. the ip/hostname to access your jenkins server. 
		so i.e. localhost or 10.1.1.23 
	</div></div>
	<label>Jenkins Server</label>
	<g:textField name="jenserver" placeholder="Jenkins Hostname"/><a id="jenserverHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jenserverHeader','#jenserver','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<div id="jensport" style="display: none;"><div class="message" role="status">
		Jenkins Server Port i.e. 8080 or 9090 or if running on 80 don't set a port 
	</div></div>
	<label>Jenkins Port</label>
	<g:textField name="jensport" placeholder="port"/><a id="jensportHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensportHeader','#jensport','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<div id="jensprefix" style="display: none;"><div class="message" role="status">
		Jenkins prefix: Have a prefix? like jenkins? then define it, so far making:
		http://yourhost:your_port/your_prefix/ 
	</div></div>
	<label>Jenkins Prefix</label>
	<g:textField name="jensprefix" placeholder="prefix" value=""/><a id="jensprefixHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensprefixHeader','#jensprefix','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<div id="jensfolder" style="display: none;"><div class="message" role="status">
		Jenkins folder: usually job, so far making:
		http://yourhost:your_port/your_prefix/job/ 
	</div></div>
	<label>Jenkins Folder</label>
	<g:textField name="jensfolder"  value="job"/><a id="jensfolderHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensfolderHeader','#jensfolder','');
	</g:javascript>
	</div>


	<div  class="fieldcontain">
	<div id="jensjob" style="display: none;"><div class="message" role="status">
		Jenkins Job: Your jenkins job name, so far making:
		http://yourhost:your_port/your_prefix/job/your_job_name 
	</div></div>
	<label>Jenkins Job</label>
	<g:textField name="jensjob" /><a id="jensjobHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jensjobHeader','#jensjob','');
	</g:javascript>
	</div>
	
	
	<div  class="fieldcontain">
	<div id="jensuser" style="display: none;"><div class="message" role="status">
		username: If Jenkins can authenticate/requires it if not leave token blank
	</div></div>
	<label for="Username">Username</label>
		<g:textField name="jensuser" placeholder="Jenkins username"/><a id="jensuserHeader"><i>Help?</i></a>
		<g:javascript>
	toggleBlock('#jensuserHeader','#jensuser','');
	</g:javascript>
	</div>

	<div  class="fieldcontain">
	<div id="jenspass" style="display: none;"><div class="message" role="status">
		password: Your apiToken from http://your_jenkins:{port}/user/username/configure
	</div></div>
	<label>Password</label>
	<g:passwordField name="jenspass" placeholder="Jenkins API Token"/><a id="jenspassHeader"><i>Help?</i></a>
	<g:javascript>
	toggleBlock('#jenspassHeader','#jenspass','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<div id="jenschoice" style="display: none;"><div class="message" role="status">
		Your Choice dashboard displays build history or trigger a build
	</div></div>
	<label>Choice</label>
	<g:select name="jenschoice" from="${['dashboard':'Dashboard','build':'Build']}" 
	 optionKey="key" optionValue="value" class="many-to-one"/><a id="jenschoiceHeader"><i>Help?</i></a>
	 <g:javascript>
	toggleBlock('#jenschoiceHeader','#jenschoice','');
	</g:javascript>
	</div>
	
	<div  class="fieldcontain">
	<label></label>
	<g:submitButton name="submit" value="Connect to Jenkins" />
	</div>
	</g:form>
 </div>
  </div></div>
	
	