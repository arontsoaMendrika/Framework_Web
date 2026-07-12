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
        response.setContentType("text/html;charset=UTF-8");
        
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
                
                Object resultatInvocation = method.invoke(controleurInstance);

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
            try (PrintWriter out = response.getWriter()) {
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