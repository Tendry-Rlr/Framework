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
import java.lang.RuntimeException;

import annotation.controller.UrlMapping;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class Util {
    public void classes_annotes(String nomPackage, Class<? extends Annotation> annotation,
            HashMap<UrlMethod, MappingUrl> listeURL, BeanDefinitionRegistry registry) {

        List<String> liste = new ArrayList<>();

        for (Class<?> claz : this.scanner_packages(nomPackage)) {
            if (claz.isAnnotationPresent(annotation)) {
                // définition du Bean pour Spring 
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(claz);

                //Injection automatique par type pour activer le @Autowired interne
                beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);

                // On génère un nom unique pour le bean (ex: "helloController")
                String beanName = claz.getSimpleName().substring(0, 1).toLowerCase()
                        + claz.getSimpleName().substring(1);

                // enregistrement dans Spring 
                registry.registerBeanDefinition(beanName, beanDefinition);
                liste.add(claz.getSimpleName());

                // ajout dans MAP
                for (Method method : claz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping annotationURL = method.getAnnotation(UrlMapping.class);

                        // verification de en cas de doublon url
                        UrlMethod urlMethod = new UrlMethod(annotationURL.url(), annotationURL.methode());
                        if (listeURL.containsKey(urlMethod)) {
                            throw new RuntimeException("Erreur : url duplique pour : " + urlMethod.getUrl());
                        }
                        listeURL.put(urlMethod, new MappingUrl(claz, method));
                    }
                }
            }
        }
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
