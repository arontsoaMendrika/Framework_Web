package main.java.util;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String viewName; 
    private Map<String, Object> model = new HashMap<>();

    public ModelAndView() {}

    public ModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    // Permet d'ajouter des objets à envoyer à la JSP
    public void addObject(String attributeName, Object attributeValue) {
        this.model.put(attributeName, attributeValue);
    }
}