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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.MappingUrl;
import tools.UrlMethod;
import tools.Util;

public class RouteServlet extends HttpServlet {
    private String nomProjet;
    private List<String> classes;
    HashMap<UrlMethod, MappingUrl> listeURL;

    public void init() throws ServletException {
        listeURL = new HashMap<>();

        String scan = this.getInitParameter("scanController");
        nomProjet = this.getInitParameter("projectName");

        Util util = new Util();
        classes = util.classes_annotes(scan, FrontController.class, listeURL);

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
        // afficher les classes annotees
        PrintWriter out = res.getWriter();
        out.println("Taille du map : " + listeURL.size());

        for (int i = 0; i < classes.size(); i++) {
            out.println(classes.get(i));
        }

        // verification si page
        if (texte.contains(".html") || texte.contains(".jsp")) {
            RequestDispatcher dispatcher = req.getServletContext().getNamedDispatcher("default");
            dispatcher.forward(req, res);
        }

        else {
            if (texte.equals("App-Test")) {
                texte = "";
            }

            String message = "";
            for (Map.Entry<UrlMethod, MappingUrl> entry : listeURL.entrySet()) {
                if (entry.getKey().getUrl().equals(texte)) {
                    UrlMapping annotation = entry.getValue().getMethod().getAnnotation(UrlMapping.class);
                    message = "Classe : " + entry.getValue().getClaz().getSimpleName() + "; URL : " + annotation.url()
                            + "; METHOD : " + entry.getValue().getMethod().getName()
                            + "; TYPE : " + entry.getKey().getTypeMethode();
                    out.print(message);

                    
                    // executer la methode
                    // Object instance = entry.getValue().getClaz().newInstance();
                    // entry.getValue().getMethod().invoke(instance);

                    break;
                }
            }

            if (message == "") {
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
