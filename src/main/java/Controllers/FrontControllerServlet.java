package main.java.Controllers;

import java.io.*;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.util.*;

import java.lang.reflect.Method;

import main.java.util.*;
import main.java.annotation.RestAPI;
import com.google.gson.Gson;
import main.java.annotation.Authenticated;
import main.java.annotation.Role;

@jakarta.servlet.annotation.MultipartConfig
public class FrontControllerServlet extends HttpServlet {

    private HashMap<String, Method> mappingUrls;
    private String prefix;
    private String suffix;
    private String authKey;
    private String roleKey;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        super.init();
        this.mappingUrls = (HashMap<String, Method>) getServletContext().getAttribute("tableRoutage");

        this.prefix = getServletConfig().getInitParameter("prefix");
        this.suffix = getServletConfig().getInitParameter("suffix");
        this.authKey = getServletConfig().getInitParameter("authKey");
        this.roleKey = getServletConfig().getInitParameter("roleKey");

        if (this.authKey == null)
            this.authKey = "user";
        if (this.roleKey == null)
            this.roleKey = "role";
        // Sprint 9 : si le listener n'a pas créé l'ApplicationContext, on le crée ici
        if (getServletContext().getAttribute("applicationContext") == null) {
            getServletContext().setAttribute("applicationContext", new ApplicationContext());
        }

        if (this.mappingUrls == null) {
            throw new ServletException("La table de routage n'a pas pu être récupérée du ServletContextListener !");
        }
        System.out.println("Servlet prête et connectée à la table de routage. Configuration : prefix = " + this.prefix
                + ", suffix = " + this.suffix);
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.equals(String.class) ||
                clazz.isPrimitive() ||
                clazz.equals(Integer.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Boolean.class);
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (targetType.equals(int.class) || targetType.equals(Integer.class)) {
            return Integer.parseInt(value);
        }
        if (targetType.equals(double.class) || targetType.equals(Double.class)) {
            return Double.parseDouble(value);
        }
        if (targetType.equals(boolean.class) || targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }

    private Object objectInObject(Class<?> clazz, HttpServletRequest request, String prefix) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();

            String paramName = (prefix.isEmpty()) ? field.getName() : prefix + "." + field.getName();

            if (isSimpleType(fieldType)) {
                String fieldValue = request.getParameter(paramName);
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    field.set(instance, convertValue(fieldValue, fieldType));
                }
            } else {
                Object nestedInstance = objectInObject(fieldType, request, paramName);
                field.set(instance, nestedInstance);
            }
        }
        return instance;
    }

    private String getUploadPath() {
        Properties properties = new Properties();
        try (InputStream input = getServletContext().getClassLoader().getResourceAsStream("upload.properties")) {
            if (input != null) {
                properties.load(input);
                return properties.getProperty("upload.path");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return System.getProperty("java.io.tmpdir");
    }

    private boolean checkAccess(Method method, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Class<?> clazz = method.getDeclaringClass();

        Role role = method.isAnnotationPresent(Role.class)
                ? method.getAnnotation(Role.class)
                : clazz.getAnnotation(Role.class);

        boolean needAuth = role != null
                || method.isAnnotationPresent(Authenticated.class)
                || clazz.isAnnotationPresent(Authenticated.class);

        if (!needAuth)
            return true; // méthode publique

        HttpSession session = request.getSession(false); // false : ne pas créer de session
        if (session == null || session.getAttribute(authKey) == null) {
            sendAccessError(method, response, 401, "Authentification requise");
            return false;
        }

        if (role != null && !hasRole(session.getAttribute(roleKey), role.value())) {
            sendAccessError(method, response, 403, "Acces refuse : role insuffisant");
            return false;
        }
        return true;
    }

    private boolean hasRole(Object userRole, String[] allowed) {
        if (userRole == null)
            return false;
        List<String> userRoles = new ArrayList<>();
        if (userRole instanceof String[]) {
            userRoles.addAll(Arrays.asList((String[]) userRole));
        } else if (userRole instanceof Collection) {
            for (Object o : (Collection<?>) userRole)
                userRoles.add(String.valueOf(o));
        } else {
            userRoles.add(userRole.toString());
        }
        for (String r : allowed) {
            for (String u : userRoles) {
                if (r.equalsIgnoreCase(u.trim()))
                    return true;
            }
        }
        return false;
    }

    private void sendAccessError(Method method, HttpServletResponse response,
            int status, String message) throws IOException {
        response.setStatus(status);
        if (method.isAnnotationPresent(RestAPI.class)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"error\": \"" + message + "\"}");
        } else {
            response.sendError(status, message);
        }
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

            if (!checkAccess(method, request, response)) {
                return;
            }

            try {
                Object controleurInstance = clazz.getDeclaredConstructor().newInstance();

                // SPRINT 7 : BINDING DES PARAMÈTRES (AVEC @RequestParam SUR LA MÉTHODE)

                java.lang.reflect.Parameter[] parameters = method.getParameters();
                Object[] parameterValues = new Object[parameters.length];

                MySession sessionToSync = null;

                // Parcours de chaque paramètre de la méthode
                for (int i = 0; i < parameters.length; i++) {
                    java.lang.reflect.Parameter param = parameters[i];
                    Class<?> paramType = param.getType();

                    if (isSimpleType(paramType)) {
                        String paramName = param.getName(); // maka anle paramatre ex: "nom", "age"
                        String requestValue = null;

                        // Si le paramètre est annoté avec @RequestParam
                        if (param.isAnnotationPresent(main.java.annotation.RequestParam.class)) {
                            requestValue = request.getParameter(paramName);
                        }

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
                    } else if (param.getType().equals(MapFile.class)) {
                        Part filePart = request.getPart(param.getName());
                        if (filePart != null && filePart.getSize() > 0) {
                            String submittedFileName = filePart.getSubmittedFileName();
                            String extension = "";
                            int j = submittedFileName.lastIndexOf('.');
                            if (j > 0) {
                                extension = submittedFileName.substring(j + 1);
                            }
                            byte[] bytes = filePart.getInputStream().readAllBytes();

                            String uploadPath = getUploadPath();
                            File uploadDir = new File(uploadPath);
                            if (!uploadDir.exists())
                                uploadDir.mkdirs();

                            File fileToSave = new File(uploadDir, submittedFileName);
                            filePart.write(fileToSave.getAbsolutePath());

                            MapFile mapFile = new MapFile();
                            mapFile.setNom(submittedFileName);
                            mapFile.setExtension(extension);
                            mapFile.setBytes(bytes);

                            parameterValues[i] = mapFile;
                        } else {
                            parameterValues[i] = null;
                        }
                    } else if (param.getType().isArray() || List.class.isAssignableFrom(param.getType())) {
                        Collection<Part> parts = request.getParts();
                        List<MapFile> mapFiles = new ArrayList<>();

                        for (Part filePart : parts) {
                            if (filePart.getName().equals(param.getName()) && filePart.getSize() > 0) {
                                String submittedFileName = filePart.getSubmittedFileName();
                                String extension = "";
                                int j = submittedFileName.lastIndexOf('.');
                                if (j > 0) {
                                    extension = submittedFileName.substring(j + 1);
                                }
                                byte[] bytes = filePart.getInputStream().readAllBytes();

                                String uploadPath = getUploadPath();
                                File uploadDir = new File(uploadPath);
                                if (!uploadDir.exists())
                                    uploadDir.mkdirs();

                                File fileToSave = new File(uploadDir, submittedFileName);
                                filePart.write(fileToSave.getAbsolutePath());

                                MapFile mapFile = new MapFile();
                                mapFile.setNom(submittedFileName);
                                mapFile.setExtension(extension);
                                mapFile.setBytes(bytes);

                                mapFiles.add(mapFile);
                            }
                        }

                        if (param.getType().isArray()) {
                            parameterValues[i] = mapFiles.toArray(new MapFile[0]);
                        } else {
                            parameterValues[i] = mapFiles;
                        }
                    } // SPRINT 9 : GESTION DE LA SESSION
                    else if (param.getType().equals(MySession.class)) {
                        HttpSession nativeSession = request.getSession(true); // avant l'écriture de la réponse
                        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                                .getAttribute("applicationContext");
                        sessionToSync = appCtx.getOrCreate(nativeSession);
                        parameterValues[i] = sessionToSync;

                    } else if (HttpServletRequest.class.isAssignableFrom(paramType)) {
                        parameterValues[i] = request;

                    } else if (HttpServletResponse.class.isAssignableFrom(paramType)) {
                        parameterValues[i] = response;

                    } else {
                        // Objets dans objets
                        try {
                            parameterValues[i] = objectInObject(paramType, request, "");
                        } catch (Exception e) {
                            throw new ServletException(
                                    "Erreur lors du binding de l'objet : " + paramType.getName(), e);
                        }
                    }

                }
                Object resultatInvocation;
                try {
                    resultatInvocation = method.invoke(controleurInstance, parameterValues);
                } finally {
                    // Reporte la session dans la vraie HttpSession, même en cas d'exception
                    if (sessionToSync != null) {
                        sessionToSync.syncToHttpSession();
                    }
                }

                if (method.isAnnotationPresent(RestAPI.class)) {
                    response.setContentType("application/json;charset=UTF-8");

                    String jsonResponse = "";

                    // Application de la consigne : String direct, sinon toJson
                    if (resultatInvocation instanceof String) {
                        jsonResponse = (String) resultatInvocation;
                    } else {
                        Gson gson = new Gson();
                        if (resultatInvocation instanceof ModelAndView) {
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
                        out.println(
                                "<p>La méthode " + method.getName() + "() n'a pas retourné un objet ModelAndView.</p>");
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
                out.println("<h2>Aucun mapping trouvé pour la combinaison : <span style='color:red;'>" + cleRecherchee
                        + "</span></h2>");
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