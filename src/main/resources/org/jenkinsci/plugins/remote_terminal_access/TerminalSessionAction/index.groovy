package org.jenkinsci.plugins.remote_terminal_access.TerminalSessionAction;

def l=namespace(lib.LayoutTagLib)
def st=namespace("jelly:stapler")

l.layout {
    def title = _("Interactive Terminal")
    l.main_panel(title:title) {
        h1 title
        st.adjunct includes:"org.kohsuke.ajaxterm"

        div(id:"term",class:"ajaxterm")

        script(type:"text/javascript", """
            Behaviour.addLoadEvent(function(){
                t=new ajaxterm.Terminal("term",{width:80,height:25,endpoint:"./u"});
            });
""")
    }
}