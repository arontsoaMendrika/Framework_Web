package main.java.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import main.java.util.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[LISTENER] Démarrage de l'application : Début du scan des annotations...");
        ServletContext context = sce.getServletContext();
        
        HashMap<String, Method> mappingUrls = new HashMap<>();

        try {
            List<Class<?>> classes = Util.getClassesInPackage("main.java");
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(main.java.annotation.Controller.class)) {
                    System.out.println("Controller trouvé : " + clazz.getName());

                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(main.java.annotation.UrlMapping.class)) {
                            main.java.annotation.UrlMapping mapping = method.getAnnotation(main.java.annotation.UrlMapping.class);
                            
                            String cleRoute = mapping.method().name() + ":" + mapping.value();

                            if (mappingUrls.containsKey(cleRoute)) {
                                Method methodeExistante = mappingUrls.get(cleRoute);
                                String messageErreur = String.format(
                                        "Conflit de mapping d'URL pour la route : %s. Méthode existante : %s, Méthode conflictuelle : %s",
                                        cleRoute, methodeExistante.getName(), method.getName());
                                throw new RuntimeException(messageErreur);
                            }
                            
                            mappingUrls.put(cleRoute, method);
                            System.out.println("Mapping enregistré -> " + cleRoute);
                        }
                    }
                }
            }

            context.setAttribute("tableRoutage", mappingUrls);
            System.out.println("[LISTENER] Table de routage partagée avec succès !");

        } catch (Exception e) {
            System.err.println("[LISTENER] Erreur fatale au démarrage !");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[LISTENER] Arrêt de l'application.");
    }
}