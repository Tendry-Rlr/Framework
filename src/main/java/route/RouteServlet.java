package route;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotation.controller.FrontController;
import annotation.controller.UrlMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.MappingUrl;
import tools.ModelAndView;
import tools.UrlMethod;
import org.springframework.context.ApplicationContext;

public class RouteServlet extends HttpServlet {
    private String nomProjet;
    HashMap<UrlMethod, MappingUrl> listeURL;
    private String prefixe;
    private String suffixe;
    private ApplicationContext appContext;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.listeURL = (HashMap<UrlMethod, MappingUrl>) servletContext.getAttribute("urlMap");
        this.nomProjet = (String) servletContext.getAttribute("projectName");
        this.prefixe = (String) servletContext.getAttribute("prefixe");
        this.suffixe = (String) servletContext.getAttribute("suffixe");
        this.appContext = (ApplicationContext) servletContext.getAttribute("springContext");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String lien = req.getRequestURL().toString();

        String[] parts = lien.split(nomProjet);
        String texte = parts[parts.length - 1];

        // verification si page
        if (texte.contains(".html") || texte.contains(".jsp")) {
            RequestDispatcher dispatcher = req.getServletContext().getNamedDispatcher("default");
            dispatcher.forward(req, res);
        } else {
            if (texte.equals("App-Test")) {
                texte = "";
            }

            String message = "";
            for (Map.Entry<UrlMethod, MappingUrl> entry : listeURL.entrySet()) {
                // afficher la classe associe a l'URL
                if (entry.getKey().getUrl().equals(texte)
                        && entry.getKey().getTypeMethode().equalsIgnoreCase(req.getMethod())) {
                    UrlMapping annotation = entry.getValue().getMethod().getAnnotation(UrlMapping.class);

                    // executer la methode
                    try {
                        // La méthode Spring :
                        Object instance = entry.getValue().getClaz().getDeclaredConstructor().newInstance();

                        Class<?>[] parameterTypes = entry.getValue().getMethod().getParameterTypes();

                        Object[] arguments = new Object[parameterTypes.length];

                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (parameterTypes[i] == HttpServletRequest.class) {
                                arguments[i] = req; // On injecte la requête HTTP
                            } else if (parameterTypes[i] == HttpServletResponse.class) {
                                arguments[i] = res; // On injecte la réponse HTTP si besoin
                            } else {
                                arguments[i] = null; // Valeur par défaut pour les autres types
                            }
                        }

                        // 4. On exécute la méthode en lui passant l'instance et ses arguments
                        Object retour = entry.getValue().getMethod().invoke(instance, arguments);

                        if (retour instanceof ModelAndView modele) {
                            for (Map.Entry<String, Object> entries : modele.getModele().entrySet()) {
                                req.setAttribute(entries.getKey(), entries.getValue());
                            }

                            RequestDispatcher dispatcher = req
                                    .getRequestDispatcher(this.prefixe + modele.getView() + this.suffixe);
                            dispatcher.forward(req, res);
                        } else {
                            PrintWriter out = res.getWriter();

                            message = "Classe : " + entry.getValue().getClaz().getSimpleName() + "; URL : "
                                    + annotation.url()
                                    + "; METHOD : " + entry.getValue().getMethod().getName()
                                    + "; TYPE : " + entry.getKey().getTypeMethode();
                            out.print(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // Erreur si le constructeur pose problème
                    }
                    break;
                }
            }

            if (message.equals("")) {
                PrintWriter out = res.getWriter();

                for (Map.Entry<UrlMethod, MappingUrl> entry : listeURL.entrySet()) {
                    UrlMapping annotation = entry.getValue().getMethod().getAnnotation(UrlMapping.class);
                    message = "Classe : " + entry.getValue().getClaz().getSimpleName() + "; URL : "
                            + annotation.url()
                            + "; METHOD : "
                            + entry.getValue().getMethod().getName()
                            + "; TYPE : " + entry.getKey().getTypeMethode();
                    out.println(message);
                }
            }
        }
    }
}
