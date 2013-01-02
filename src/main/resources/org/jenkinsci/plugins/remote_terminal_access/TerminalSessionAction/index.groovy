package org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction;

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
                f.submit(value:_("Launch a terminal"))
            }
        }

    }
}