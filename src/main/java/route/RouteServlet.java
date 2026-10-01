package route;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotation.controller.FrontController;
import annotation.controller.UrlMapping;
import annotation.controller.WebAPI;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
                        Object instance = entry.getValue().getClaz().getDeclaredConstructor().newInstance();

                        Class<?>[] parameterTypes = entry.getValue().getMethod().getParameterTypes();
                        Object[] arguments = new Object[parameterTypes.length];

                        // for (int i = 0; i < parameterTypes.length; i++) {
                        // if (parameterTypes[i] == HttpServletRequest.class) {
                        // arguments[i] = req;
                        // } else if (parameterTypes[i] == HttpServletResponse.class) {
                        // arguments[i] = res;
                        // } else {
                        // arguments[i] = null;
                        // }
                        // }

                        matchingVariableInput(req, arguments, entry.getValue().getMethod());

                        // On exécute la méthode en lui passant l'instance et ses arguments
                        Object retour = entry.getValue().getMethod().invoke(instance, arguments);
                        PrintWriter out = res.getWriter();

                        if (entry.getValue().getMethod().isAnnotationPresent(WebAPI.class)) {
                            if (retour instanceof String retourStr) {
                                out.print(retourStr);
                            } else {
                                ObjectMapper mapper = new ObjectMapper();
                                String jsonResultat = mapper.writeValueAsString(retour);
                                out.println(jsonResultat);
                            }
                            return;
                        }

                        if (retour instanceof ModelAndView modele) {
                            for (Map.Entry<String, Object> entries : modele.getModele().entrySet()) {
                                req.setAttribute(entries.getKey(), entries.getValue());
                            }

                            RequestDispatcher dispatcher = req
                                    .getRequestDispatcher(this.prefixe + modele.getView() + this.suffixe);
                            dispatcher.forward(req, res);
                        } else {

                            message = "Classe : " + entry.getValue().getClaz().getSimpleName() + "; URL : "
                                    + annotation.url()
                                    + "; METHOD : " + entry.getValue().getMethod().getName()
                                    + "; TYPE : " + entry.getKey().getTypeMethode();
                            out.print(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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

    protected void matchingVariableInput(HttpServletRequest req, Object[] arguments, Method method) {

        Parameter[] parameters = method.getParameters();
        if (arguments.length == 0) {
            return;
        }
        Enumeration<String> inputsEnums = req.getParameterNames();
        List<String> inputs = Collections.list(inputsEnums);

        // verifier si le nom des arguments sont egais au nom des inputs
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Class<?> type = parameters[i].getType();

            boolean found = false;

            String value = "";
            for (String input : inputs) {
                if (input.equalsIgnoreCase(paramName)) {
                    value = req.getParameter(input);
                    arguments[i] = convertType(value, type);
                    found = true;
                }
            }

            if (found) {
                continue;
            } else {
                arguments[i] = null;
            }
        }
    }

    private Object convertType(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // 1. Chaînes de caractères
        if (targetType == String.class) {
            return value;
        }
        // 2. Nombres entiers
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        // 3. Nombres décimaux
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        // 4. Booléens
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

}
