package route;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import annotation.controller.Authentification;
import annotation.controller.Role;
import annotation.controller.UrlMapping;
import annotation.controller.WebAPI;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import session.MySession;
import tools.Convertion;
import tools.MappingUrl;
import tools.ModelAndView;
import tools.UrlMethod;
import org.springframework.context.ApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@MultipartConfig
public class RouteServlet extends HttpServlet {
    private String nomProjet;
    HashMap<UrlMethod, MappingUrl> listeURL;
    private String prefixe;
    private String suffixe;
    private ApplicationContext appContext;
    private Map<String, MySession> sessions;
    private String authentification;
    private String role;
    private Map<String, Object> instanceController;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        ServletContext servletContext = getServletContext();
        this.listeURL = (HashMap<UrlMethod, MappingUrl>) servletContext.getAttribute("urlMap");
        this.nomProjet = (String) servletContext.getAttribute("projectName");
        this.prefixe = (String) servletContext.getAttribute("prefixe");
        this.suffixe = (String) servletContext.getAttribute("suffixe");
        this.appContext = (ApplicationContext) servletContext.getAttribute("springContext");
        this.sessions = (Map<String, MySession>) servletContext.getAttribute("sessions");
        this.authentification = this.getInitParameter("authentification-name");
        this.role = this.getInitParameter("role-name");
        this.instanceController = (Map<String, Object>) servletContext.getAttribute("instanceControllers");
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

                    try {
                        if (entry.getValue().getMethod().isAnnotationPresent(Authentification.class)) {
                            try {
                                this.verifyAuthentification(req);
                            } catch (RuntimeException e) {
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                                return;
                            }
                        }

                        if (entry.getValue().getMethod().isAnnotationPresent(Role.class)) {
                            Role roleAnnotation = entry.getValue().getMethod().getAnnotation(Role.class);
                            try {
                                this.verifyRole(req, res, roleAnnotation.roles());
                            } catch (RuntimeException e) {
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                                return;
                            }
                        }

                        // implementation de l'instance pour le signleton
                        Object instance = null;
                        String nomController = entry.getValue().getClaz().getSimpleName();

                        if (instanceController.get(nomController) == null) {
                            instanceController.put(nomController,
                                    entry.getValue().getClaz().getDeclaredConstructor().newInstance());
                            System.out.println("Nouvelle instance de :" + nomController);
                        }
                        else {
                            System.out.println("Intance deja presente pour : " + nomController);
                        }
                        instance = instanceController.get(nomController);

                        // executer la methode

                        Class<?>[] parameterTypes = entry.getValue().getMethod().getParameterTypes();
                        Object[] arguments = new Object[parameterTypes.length];

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
                        typeRetour(retour, message, out, annotation, entry, req, res);
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

    public void verifyAuthentification(HttpServletRequest req) {
        HttpSession sessionHttp = req.getSession();
        String sessionId = sessionHttp.getId();
        System.out.println("Session id : " + sessionId);

        MySession mySession = sessions.get(sessionId);

        if (mySession == null) {
            System.out.println("Initialisation de la session");
            mySession = new MySession();
            mySession.chargerSession(sessionHttp);
            sessions.put(sessionId, mySession);
        }

        if (mySession.getValue(this.authentification) == null) {
            System.out.println("Pas encore authentifiee");
            throw new RuntimeException("Pas encore authentifiee");
        }
    }

    public void verifyRole(HttpServletRequest req, HttpServletResponse res, String[] roles) {
        try {
            this.verifyAuthentification(req);

            HttpSession sessionHttp = req.getSession();
            String sessionId = sessionHttp.getId();

            MySession mySession = sessions.get(sessionId);

            if (mySession == null) {
                System.out.println("Initialisation de la session");
                mySession = new MySession();
                mySession.chargerSession(sessionHttp);
                sessions.put(sessionId, mySession);
            }

            if (mySession.getValue(this.role) == null) {
                throw new RuntimeException("Pas de role dans session");
            } else {
                boolean present = false;
                String roleValue = (String) mySession.getValue(this.role);
                for (String r : roles) {
                    if (r.equals(roleValue)) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    throw new RuntimeException("Role non accordee");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    protected void typeRetour(Object retour, String message, PrintWriter out, UrlMapping annotation,
            Map.Entry<UrlMethod, MappingUrl> entry, HttpServletRequest req,
            HttpServletResponse res) {
        if (retour instanceof ModelAndView modele) {
            for (Map.Entry<String, Object> entries : modele.getModele().entrySet()) {
                req.setAttribute(entries.getKey(), entries.getValue());
            }

            RequestDispatcher dispatcher = req
                    .getRequestDispatcher(this.prefixe + modele.getView() + this.suffixe);
            try {
                dispatcher.forward(req, res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            message = "Classe : " + entry.getValue().getClaz().getSimpleName() + "; URL : "
                    + annotation.url()
                    + "; METHOD : " + entry.getValue().getMethod().getName()
                    + "; TYPE : " + entry.getKey().getTypeMethode();
            out.print(message);
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
            if (type.getSimpleName().equalsIgnoreCase("MySession")) {
                System.out.println("session found");
                arguments[i] = Convertion.gererSession(req, this.sessions);
                continue;
            }
            if (type.isArray()) {
                arguments[i] = Convertion.bindList(type, req, parameter.getName());
            } else if (!type.isPrimitive() && type != String.class && !Number.class.isAssignableFrom(type)) {
                arguments[i] = Convertion.bindComplexObject(type, req, parameter.getName());
            } else {
                String value = req.getParameter(parameter.getName());
                arguments[i] = Convertion.convertType(value, type);
            }
        }
    }
}
