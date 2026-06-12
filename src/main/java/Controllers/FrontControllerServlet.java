package main.java.Controllers;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;    

public class FrontControllerServlet extends HttpServlet {
 protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

   response.setContentType("text/html;charset=UTF-8");
    try(PrintWriter out = response.getWriter()) {
            String uri = request.getRequestURI();
            out.println("<html><body>");
            out.println("<h1>URI: " + uri + "</h1>");
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
}