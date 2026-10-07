package main.java.Controllers;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

import main.java.util.ModelAndView;
import main.java.annotation.RestAPI; // 👈 1. Importation de l'annotation RestAPI
import com.google.gson.Gson;

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
    
    // SPRINT 7 : BINDING DES PARAMÈTRES (AVEC @RequestParam SUR LA MÉTHODE)
    
    java.lang.reflect.Parameter[] parameters = method.getParameters();
    Object[] parameterValues = new Object[parameters.length];

    // Parcours de chaque paramètre de la méthode
    for (int i = 0; i < parameters.length; i++) {
        java.lang.reflect.Parameter param = parameters[i];
        String paramName = param.getName(); // maka anle paramatre ex: "nom", "age"
        Class<?> paramType = param.getType();

        String requestValue = null;

        // Si le paramètre est annoté avec @RequestParam
        if (param.isAnnotationPresent(main.java.annotation.RequestParam.class)) {
            requestValue = request.getParameter(paramName);
        }

        // Conversion du type String vers le type cible du paramètre Java
        if (requestValue != null && !requestValue.trim().isEmpty()) {
            try {
                if (paramType == int.class || paramType == Integer.class) {
                    parameterValues[i] = Integer.parseInt(requestValue);
                } else if (paramType == double.class || paramType == Double.class) {
                    parameterValues[i] = Double.parseDouble(requestValue);
                } else if (paramType == boolean.class || paramType == Boolean.class) {
                    parameterValues[i] = Boolean.parseBoolean(requestValue);
                } else {
                    parameterValues[i] = requestValue; 
                }
            } catch (NumberFormatException e) {
                // Gestion des erreurs de conversion
                if (paramType == int.class) {
                    parameterValues[i] = 0;
                } else if (paramType == double.class) {
                    parameterValues[i] = 0.0;
                } else if (paramType == boolean.class) {
                    parameterValues[i] = false;
                } else {
                    parameterValues[i] = null;
                }
            }
        } else {
            // Valeurs par défaut si le paramètre est absent de la requête
            if (paramType == int.class) {
                parameterValues[i] = 0;
            } else if (paramType == double.class) {
                parameterValues[i] = 0.0;
            } else if (paramType == boolean.class) {
                parameterValues[i] = false;
            } else {
                parameterValues[i] = null;
            }
        }
    }

    // Exécution dynamique avec le tableau de paramètres rempli
    Object resultatInvocation = method.invoke(controleurInstance, parameterValues);
    
    if (method.isAnnotationPresent(RestAPI.class)) {
                    response.setContentType("application/json;charset=UTF-8");
                    
                    String jsonResponse = "";
                    
                    // Application de la consigne : String direct, sinon toJson
                    if (resultatInvocation instanceof String) {
                        jsonResponse = (String) resultatInvocation;
                    } else {
                        Gson gson = new Gson();
                        if(resultatInvocation instanceof ModelAndView) {
                            ModelAndView mv = (ModelAndView) resultatInvocation;
                            jsonResponse = gson.toJson(mv.getModel());
                        } else {
                            jsonResponse = gson.toJson(resultatInvocation);
                        }
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
                        out.println("<html><body>");
                        out.println("<h1>Erreur d'architecture</h1>");
                        out.println("<p>La méthode " + method.getName() + "() n'a pas retourné un objet ModelAndView.</p>");
                        out.println("</body></html>");
                    }
                }

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

    // private String toJson(Object obj) {
    //     if (obj instanceof ModelAndView) {
    //         ModelAndView mv = (ModelAndView) obj;
    //         return mapToJson(mv.getModel());
    //     } else if (obj instanceof Map) {
    //         return mapToJson((Map<String, Object>) obj);
    //     }
    //     return "{}";
    // }

    // @SuppressWarnings("unchecked")
    // private String mapToJson(Map<String, Object> map) {
    //     if (map == null || map.isEmpty()) {
    //         return "{}";
    //     }
        
    //     StringBuilder json = new StringBuilder();
    //     json.append("{");
        
    //     int i = 0;
    //     for (Map.Entry<String, Object> entry : map.entrySet()) {
    //         json.append("\"").append(entry.getKey()).append("\":");
            
    //         Object val = entry.getValue();
    //         if (val instanceof Number || val instanceof Boolean) {
    //             json.append(val);
    //         } else {
    //             json.append("\"").append(val != null ? val.toString() : "null").append("\"");
    //         }
            
    //         if (i < map.size() - 1) {
    //             json.append(",");
    //         }
    //         i++;
    //     }
        
    //     json.append("}");
    //     return json.toString();
    // }

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