package route;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import annotation.controller.UrlMapping;
import annotation.controller.WebAPI;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.Convertion;
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

                        // matchingVariableInput(req, res, arguments, entry.getValue().getMethod());
                        matchingVariableInputObject(req, res, arguments, entry.getValue().getMethod());

                        System.out.println("Arguments prêts : " + Arrays.toString(arguments));

                        Object retour = entry.getValue().getMethod().invoke(instance, arguments);
                        PrintWriter out = res.getWriter();

                        if (entry.getValue().getMethod().isAnnotationPresent(WebAPI.class)) {
                            res.setContentType("application/json");
                            if (retour instanceof String retourStr) {
                                String json = "{\"success\":\"" + retourStr + "\"}";
                                out.print(json);
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

    protected void matchingVariableInput(HttpServletRequest req, HttpServletResponse res,
            Object[] arguments, Method method) {
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (HttpServletRequest.class.isAssignableFrom(type)) {
                arguments[i] = req;
                continue;
            }
            if (HttpServletResponse.class.isAssignableFrom(type)) {
                arguments[i] = res;
                continue;
            }

            String value = req.getParameter(parameter.getName());

            // verifier si la valeur est vide ou non
            boolean isEmpty = (value == null || value.trim().isEmpty());

            if (isEmpty) {
                arguments[i] = type.isPrimitive() ? Convertion.getDefaultValue(type) : null;
            } else {
                // Si une valeur existe, on effectue la conversion de type
                arguments[i] = Convertion.convertType(value, type);
            }
        }
    }

    protected void matchingVariableInputObject(HttpServletRequest req, HttpServletResponse res,
            Object[] arguments, Method method) {
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (HttpServletRequest.class.isAssignableFrom(type)) {
                arguments[i] = req;
                continue;
            }
            if (HttpServletResponse.class.isAssignableFrom(type)) {
                arguments[i] = res;
                continue;
            }

            if (!type.isPrimitive() && type != String.class && !Number.class.isAssignableFrom(type)) {
                Field[] fields = type.getDeclaredFields();

                try {
                    Object instance = type.getDeclaredConstructor().newInstance();

                    for (int j = 0; j < fields.length; j++) {
                        String fieldName = fields[j].getName();
                        String value = req.getParameter(fieldName);

                        String nomSetter = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        Class<?> fieldType = fields[j].getType();

                        try {
                            Method setter = type.getMethod(nomSetter, fieldType);
                            Object convertedValue = Convertion.convertType(value, fieldType);

                            // Invocation du setter sur l'instance
                            setter.invoke(instance, convertedValue);
                        } catch (NoSuchMethodException e) {
                            System.out.println("Setter non trouvé : " + nomSetter);
                        }
                    }
                    arguments[i] = instance;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                String value = req.getParameter(parameter.getName());
                // verifier si la valeur est vide ou non
                arguments[i] = Convertion.defineObject(value, type);
            }
        }
    }

}
