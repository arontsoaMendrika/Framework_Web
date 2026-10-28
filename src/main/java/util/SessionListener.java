package main.java.util;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext ctx = se.getSession().getServletContext();
        ApplicationContext appCtx = (ApplicationContext) ctx.getAttribute("applicationContext");
        if (appCtx != null) {
            appCtx.remove(se.getSession().getId());
        }
    }
}