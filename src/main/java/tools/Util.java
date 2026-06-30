package tools;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import annotation.controller.UrlMapping;

public class Util {
    public List<String> classes_annotes(String nomPackage, Class<? extends Annotation> annotation,
            HashMap<UrlMethod, MappingUrl> listeURL) {

        List<String> liste = new ArrayList<>();

        for (Class<?> claz : this.scanner_packages(nomPackage)) {
            if (claz.isAnnotationPresent(annotation)) {
                liste.add(claz.getSimpleName());

                // ajout dans MAP
                for (Method method : claz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping annotationURL = method.getAnnotation(UrlMapping.class);
                        listeURL.put(new UrlMethod(annotationURL.url(), annotationURL.methode()), new MappingUrl(claz, method));
                    }
                }
            }
        }
        return liste;
    }

    protected static String[] packages(String texte) {
        String[] parts = texte.split(";");
        return parts;
    }

    public List<Class<?>> scanner_packages(String nomPackage) {
        List<Class<?>> liste = new ArrayList<>();

        List<String> temp = new ArrayList<>();
        if (nomPackage.contains(";")) {
            String[] parts = packages(nomPackage);
            for (String pack : parts) {
                try {
                    List<String> valeurs = scan(pack);
                    temp.addAll(valeurs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                temp = scan(nomPackage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Class<?> cls;
        for (String nomClasse : temp) {
            try {
                cls = Class.forName(nomClasse);
                liste.add(cls);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return liste;
    }

    public List<String> scan(String nomDePackage) throws Exception {
        List<String> classeNames = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = nomDePackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            // Cas 1 : Les classes sont dans un dossier classique (mode développement)
            if ("file".equals(protocol)) {
                File dir = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".class"));
                    if (files != null) {
                        for (File file : files) {
                            String className = file.getName().substring(0, file.getName().length() - 6);
                            classeNames.add(nomDePackage + "." + className);
                        }
                    }
                }
            }
            // Cas 2 : Les classes sont packagées dans un fichier .jar
            else if ("jar".equals(protocol)) {
                // On se connecte au fichier JAR
                JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                JarFile jar = jarConn.getJarFile();
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // On cherche les fichiers qui commencent par notre chemin de package et
                    // finissent par .class
                    if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                        // On transforme le chemin du JAR (ex: route/RouteServlet.class) en nom de
                        // classe (ex: route.RouteServlet)
                        String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                        classeNames.add(className);
                    }
                }
            }
        }
        return classeNames;
    }
}
