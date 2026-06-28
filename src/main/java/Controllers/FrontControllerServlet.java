package main.java.Controllers;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.java.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class FrontControllerServlet extends HttpServlet {
    private List<Class<?>> controllers;

protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    try (PrintWriter out = response.getWriter()) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        boolean mappingFound = false;
        
        for (Class<?> clazz : controllers) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(main.java.annotation.UrlMapping.class)) {
                    main.java.annotation.UrlMapping mapping = method
                            .getAnnotation(main.java.annotation.UrlMapping.class);
                    String urlAssocie = mapping.value();

                    if (path.equals(urlAssocie)) {
                        mappingFound = true;

                        out.println("<html><body>");
                        out.println("<h1>URI: " + uri + "</h1>");
                        out.println("<h2>Méthodes de Contrôleurs détectés :</h2>");
                        out.println("<table border='1' cellpadding='8' style='border-collapse: collapse;'>");
                        out.println("<tr style='background-color: #f2f2f2;'><th>URL annoté</th><th>Classe contrôleur</th><th>Méthode associé</th></tr>");
                        out.println("<tr>");
                        out.println("<td><strong>" + urlAssocie + "</strong></td>");
                        out.println("<td>" + clazz.getName() + "</td>");
                        out.println("<td>" + method.getName() + "()</td>");
                        out.println("</tr>");
                        out.println("</table>");
                        out.println("</body></html>");
                        break; 
                    }
                }
            }
            if (mappingFound) {
                break; 
            }
        }
        
        if (!mappingFound) {
            out.println("<html><body>");
            out.println("<h1>URI: " + uri + "</h1>");
            out.println("<h2>Aucun mapping trouvé pour cette URI.</h2>");
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

    public void init() throws ServletException {
        try {
            controllers = new ArrayList<>();
            List<Class<?>> classes = Util.getClassesInPackage("main.java");
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(main.java.annotation.Controller.class)) {
                    controllers.add(clazz);
                    System.out.println("Controller trouvé" + clazz.getName());

                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}