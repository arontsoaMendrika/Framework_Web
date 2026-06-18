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

public class FrontControllerServlet extends HttpServlet {
    private List<Class<?>> controllers;
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try(PrintWriter out = response.getWriter()) {
                String uri = request.getRequestURI();
                out.println("<html><body>");
                out.println("<h1>URI: " + uri + "</h1>");
                out.println("<h2>Contrôleurs détectés :</h2>");
                out.println("<ul>");
                for(Class<?> controllerClass : controllers){
                    out.println("<li>" + controllerClass.getSimpleName() + "</li>");
                }
                out.println("</ul>");
                out.println("</body></html>");
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
        try{
            controllers = new ArrayList<>();
            List<Class<?>> classes = Util.getClassesInPackage("main.java");
            for(Class<?> clazz : classes){
                if(clazz.isAnnotationPresent(main.java.annotation.Controller.class)){
                    controllers.add(clazz);
                    System.out.println("Controller trouvé" + clazz.getName());

                }
            }
        }catch(Exception e){
            throw new ServletException(e);
        }
    }
}