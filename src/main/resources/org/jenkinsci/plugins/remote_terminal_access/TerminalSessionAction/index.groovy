package org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction

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
        if (sshd.actualPort>0) {
            h1 _("SSH Access")
            AbstractBuild b = request.findAncestorObject(AbstractBuild.class)
            raw _("sshBlurb",new URL(app.rootUrl).host, sshd.actualPort.toString(),
                    b.rootBuild.parent.fullName, b.number.toString())
        }
    }
}