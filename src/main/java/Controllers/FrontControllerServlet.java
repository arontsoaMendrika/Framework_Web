package main.java.Controllers;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.util.Util; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Method;

public class FrontControllerServlet extends HttpServlet {
    private HashMap<String, Method> mappingUrls = new HashMap<>();
    private List<Class<?>> controllers;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            controllers = new ArrayList<>();
            List<Class<?>> classes = Util.getClassesInPackage("main.java");
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(main.java.annotation.Controller.class)) {
                    controllers.add(clazz);
                    System.out.println("Controller trouvé" + clazz.getName());

                    Method[] methods = clazz.getDeclaredMethods();
                    for(Method method : methods) {
                        if (method.isAnnotationPresent(main.java.annotation.UrlMapping.class)) {
                            main.java.annotation.UrlMapping mapping = method.getAnnotation(main.java.annotation.UrlMapping.class);
                            mappingUrls.put(mapping.value(), method);
                        
                            String cleRoute =mapping.method().name() + ":" + mapping.value();

                            if(mappingUrls.containsKey(cleRoute)) {
                                Method methodeExistante = mappingUrls.get(cleRoute);
                                String messageErreur = String.format("Conflit de mapping d'URL pour la route : %s. Méthode existante : %s, Méthode conflictuelle : %s",
                                        cleRoute, methodeExistante.getName(), method.getName());
                                throw new ServletException(messageErreur);
                            }
                            mappingUrls.put(cleRoute, method);
                            System.out.println("Mapping enregistré ->" + cleRoute);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
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
                    method.invoke(controleurInstance);
                    out.println("<html><body>");
                    out.println("<h1>URI: " + uri + "</h1>");
                    out.println("<h2>[Succès] Méthode de Contrôleur exécutée :</h2>");
                    out.println("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
                    out.println("<tr style='background-color: #e6f7ff;'><th>URL annoté</th><th>Classe contrôleur</th><th>Méthode associée</th></tr>");
                    out.println("<tr>");
                    out.println("<td><strong>" + path + "</strong></td>");
                    out.println("<td>" + clazz.getName() + "</td>");
                    out.println("<td>" + method.getName() + "()</td>");
                    out.println("</tr>");
                    out.println("</table>");
                    out.println("</body></html>");
                    
                } catch (Exception e) {
                    throw new ServletException("Erreur lors de l'exécution de la méthode " + method.getName() + "()", e);
                }
            } else {
                out.println("<html><body>");
                out.println("<h1>URI: " + uri + "</h1>");
                out.println("<h2>Aucun mapping trouvé pour la combinaison : <span style='color:red;'>" + cleRecherchee + "</span></h2>");
                out.println("</body></html>");
            }
        }
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