package org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction

import com.sun.jmx.snmp.IPAcl.Host
import hudson.model.AbstractBuild
import hudson.model.TopLevelItem
import org.jenkinsci.main.modules.sshd.SSHD;

def f=namespace(lib.FormTagLib)
def l=namespace(lib.LayoutTagLib)
def st=namespace("jelly:stapler")

l.layout {
    def title = _("Interactive Terminal")
    l.main_panel(title:title) {
        h1 title

        if (my.hasSession()) {
            st.adjunct includes:"org.kohsuke.ajaxterm"

            div(id:"term",class:"ajaxterm")

            script(type:"text/javascript", """
                Behaviour.addLoadEvent(function(){
                    t=new ajaxterm.Terminal("term",{width:80,height:25,endpoint:"./u"});
                });
    """)
            form(method:"POST",action:"restartSession",style:"margin-top:1em") {
                f.submit(value:_("Launch another terminal"))
            }
        } else {
            p(style:"margin:1em;", _("blurb"))
            form(method:"POST",action:"startSession") {
                f.submit(value:_("Launch a terminal in this browser"))
            }
        }

        def sshd = SSHD.get()
        def port = sshd.actualPort
        if (port>0) {
            AbstractBuild b = request.findAncestorObject(AbstractBuild.class)
            def jobName = b.rootBuild.parent.fullName
            def host = new URL(app.rootUrl).host

            h1 _("SSH Access")
            raw "<style>.cmd { color:white; background-color:black; font-weight:bold; padding:1em; }</style>"

            p { raw(_("sshBlurb")) }
            raw """
<pre class=cmd>Host=*.$host
Port=$port
ProxyCommand=ssh -q -p $port $host diagnose-tunnel -suffix .$host %h
</pre>
"""
            p _("sshExample")
            raw """
<pre class=cmd>
\$ ssh $jobName.$host
\$ ssh '${b.number}#$jobName.$host'
\$ ssh 'lastFailedBuild#$jobName.$host'
</pre>
"""
            p _("whenInteractive")
        }
    }
}