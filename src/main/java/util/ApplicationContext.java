package main.java.util;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    private final Map<String, MySession> sessions = new ConcurrentHashMap<>();

    public MySession getOrCreate(HttpSession httpSession) {
        MySession mySession = sessions.computeIfAbsent(httpSession.getId(), id -> new MySession(httpSession));
        mySession.bind(httpSession);  
        return mySession;
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}