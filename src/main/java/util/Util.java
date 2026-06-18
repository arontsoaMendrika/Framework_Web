package main.java.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Util {
    public static List<Class <?>> getClassesInPackage(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        String path = packageName.replace('.','/');
        ClassLoader classloader=Thread.currentThread().getContextClassLoader();

        if(classloader == null){
            throw new StringIndexOutOfBoundsException("Impossible de recupere le classloader");
        }

        Enumeration<URL> ressources = classloader.getResources(path);
        List<File> dirs = new ArrayList<>();

        while(ressources.hasMoreElements()){
            URL ressource= ressources.nextElement();
            dirs.add(new File(ressource.getFile()));
        }

        for(File directory :dirs){
            classes.addAll(findClasses(directory,packageName));
        }

        return classes;

    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        if(!directory.exists()){
            return classes;
        }

        File[] files=directory.listFiles();
        if(files == null) return classes;

        for(File file : files){
            if(file.getName().endsWith(".class")){
                String className = packageName + '.' + file.getName().substring(0,file.getName().length()-6);
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }
}
