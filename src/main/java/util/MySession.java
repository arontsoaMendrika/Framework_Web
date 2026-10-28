package main.java.util;

import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MySession {
    private HttpSession httpSession;
    private final Map<String, Object> map = new HashMap<>();

    public MySession(HttpSession httpSession) {
        bind(httpSession);
    }

    public synchronized void bind(HttpSession httpSession) {
        this.httpSession = httpSession;
        map.clear();
        if (httpSession != null) {
            Enumeration<String> attributeNames = httpSession.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String key = attributeNames.nextElement();
                map.put(key, httpSession.getAttribute(key));
            }
        }
    }

    public synchronized void add(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("La clé d'un attribut de session ne peut pas être null.");
        }
        map.put(key, value);
    }

    public synchronized Object get(String key) {
        return map.get(key);
    }

    public synchronized void delete(String key) {
        map.remove(key);
    }

    public synchronized boolean contains(String key) {
        return map.containsKey(key);
    }

    public synchronized String getId() {
        return httpSession == null ? null : httpSession.getId();
    }

    public synchronized void syncToHttpSession() {
        if (httpSession == null) {
            return;
        }

        Set<String> currentKeys = new HashSet<>();
        Enumeration<String> attributeNames = httpSession.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            currentKeys.add(attributeNames.nextElement());
        }

        for (String key : currentKeys) {
            if (!map.containsKey(key)) {
                httpSession.removeAttribute(key);
            }
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }
}