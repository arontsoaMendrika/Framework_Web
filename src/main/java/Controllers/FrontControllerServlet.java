package main.java.Controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

import main.java.util.ModelAndView;
import main.java.annotation.RestAPI; // 👈 1. Importation de l'annotation RestAPI

public class FrontControllerServlet extends HttpServlet {

    private HashMap<String, Method> mappingUrls;
    private String prefix;
    private String suffix;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        super.init();
        this.mappingUrls = (HashMap<String, Method>) getServletContext().getAttribute("tableRoutage");
        
        this.prefix = getServletConfig().getInitParameter("prefix");
        this.suffix = getServletConfig().getInitParameter("suffix");
        if (this.prefix == null) {
            this.prefix = "/";
        }
        if (this.suffix == null) {
            this.suffix = ".jsp";
        }
        
        if (this.mappingUrls == null) {
            throw new ServletException("La table de routage n'a pas pu être récupérée du ServletContextListener !");
        }
        System.out.println("Servlet prête et connectée à la table de routage. Configuration : prefix = " + this.prefix + ", suffix = " + this.suffix);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        String requeteMethod = request.getMethod(); // GET ou POST

        String cleRecherchee = requeteMethod + ":" + path;

        if (mappingUrls.containsKey(cleRecherchee)) {
            Method method = mappingUrls.get(cleRecherchee);
            Class<?> clazz = method.getDeclaringClass();

            try {
                Object controleurInstance = clazz.getDeclaredConstructor().newInstance();
                
                Object resultatInvocation = method.invoke(controleurInstance, bindParameters(method, request));

                if (method.isAnnotationPresent(RestAPI.class)) {
                    response.setContentType("application/json;charset=UTF-8");
                    
                    String jsonResponse = "";
                    
                    // Application de la consigne : String direct, sinon toJson
                    if (resultatInvocation instanceof String) {
                        jsonResponse = (String) resultatInvocation;
                    } else {
                        jsonResponse = toJson(resultatInvocation);
                    }
                    
                    try (PrintWriter out = response.getWriter()) {
                        out.print(jsonResponse);
                    }
                    return; // Fin du traitement (pas de redirection JSP)
                }

                // SPRINT 5 : Traitement classique avec rendu JSP
                response.setContentType("text/html;charset=UTF-8");

                if (resultatInvocation instanceof ModelAndView) {
                    ModelAndView mv = (ModelAndView) resultatInvocation;
                    if (mv.getModel() != null) {
                        for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }
                    String cheminJsp = this.prefix + mv.getViewName() + this.suffix;

                    RequestDispatcher dispatcher = request.getServletContext().getRequestDispatcher(cheminJsp);

                    if (dispatcher != null) {
                        dispatcher.forward(request, response);
                        return; 
                    } else {
                        throw new ServletException("Impossible de trouver le dispatcher pour le chemin : " + cheminJsp);
                    }
                } else {
                    try (PrintWriter out = response.getWriter()) {
                        response.setContentType("text/plain;charset=UTF-8");
                        out.print(resultatInvocation == null ? "" : resultatInvocation.toString());
                    }
                }

            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName() + "()", e);
            }
        } else {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<html><body>");
                out.println("<h1>URI: " + uri + "</h1>");
                out.println("<h2>Aucun mapping trouvé pour la combinaison : <span style='color:red;'>" + cleRecherchee + "</span></h2>");
                out.println("</body></html>");
            }
        }
    }

    private Object[] bindParameters(Method method, HttpServletRequest request) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameterTypes.length];

        for (int index = 0; index < parameterTypes.length; index++) {
            String parameterName = parameters[index].getName();
            String value = request.getParameter(parameterName);
            if (value == null) {
                throw new IllegalArgumentException("Missing parameter: " + parameterName);
            }
            if (parameterTypes[index] == String.class) {
                values[index] = value;
            } else if (parameterTypes[index] == int.class || parameterTypes[index] == Integer.class) {
                try {
                    values[index] = Integer.valueOf(value);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Parameter '" + parameterName + "' must be an int");
                }
            } else {
                throw new IllegalArgumentException("Unsupported parameter type: " + parameterTypes[index].getName());
            }
        }
        return values;
    }

    private String toJson(Object obj) {
        if (obj instanceof ModelAndView) {
            ModelAndView mv = (ModelAndView) obj;
            return mapToJson(mv.getModel());
        } else if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj);
        }
        return "{}";
    }

    @SuppressWarnings("unchecked")
    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object val = entry.getValue();
            if (val instanceof Number || val instanceof Boolean) {
                json.append(val);
            } else {
                json.append("\"").append(val != null ? val.toString() : "null").append("\"");
            }
            
            if (i < map.size() - 1) {
                json.append(",");
            }
            i++;
        }
        
        json.append("}");
        return json.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}