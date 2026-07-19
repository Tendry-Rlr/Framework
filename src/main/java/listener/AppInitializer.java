package listener;

import java.util.HashMap;

import annotation.controller.FrontController;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import tools.MappingUrl;
import tools.UrlMethod;
import tools.Util;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

@WebListener
public class AppInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        String scanPackage = context.getInitParameter("scanController");
        String nomProjet = context.getInitParameter("projectName");
        String suffixe = context.getInitParameter("suffixe");
        String prefixe = context.getInitParameter("prefixe");

        if (scanPackage == null || scanPackage.isBlank()) {
            throw new IllegalStateException("Parametre d'initialisation de package dans web.xml");
        }
        if (nomProjet == null || nomProjet.isBlank()) {
            throw new IllegalStateException("Parametre d'initialisation du nom de projet dans web.xml ");
        }
        try {
            Util util = new Util();
            HashMap<UrlMethod, MappingUrl> urlMapping = new HashMap<>();
            ApplicationContext contexte = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(sce.getServletContext());
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) contexte.getAutowireCapableBeanFactory();

            util.classes_annotes(scanPackage, FrontController.class, urlMapping, registry);
            context.setAttribute("urlMap", urlMapping);
            context.setAttribute("projectName", nomProjet);
            context.setAttribute("suffixe", suffixe);
            context.setAttribute("prefixe", prefixe);

            context.setAttribute("springContext", contexte);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
