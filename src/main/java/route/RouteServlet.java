package route;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import annotation.controller.Controller;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.Util;

public class RouteServlet extends HttpServlet {
    private List<String> classes;

    public void init() throws ServletException {
        String scan = this.getInitParameter("scanController");
        
        Util util = new Util();
        classes = util.classes_annotes(scan, Controller.class);
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
        String[] parts = lien.split("/");
        String texte = parts[parts.length - 1];

        // afficher les classes annotees
        PrintWriter out = res.getWriter();
        for (int i = 0; i < classes.size(); i++) {
            out.println(classes.get(i));
        }

        // verification si page
        // if (texte.contains(".html") || texte.contains(".jsp")) {
        //     RequestDispatcher dispatcher = req.getServletContext().getNamedDispatcher("default");
        //     dispatcher.forward(req, res);
        // }

        // else {
        //     if (texte.equals("App-Test")) {
        //         texte = "";
        //     }
        //     PrintWriter out = res.getWriter();
        //     out.println(texte);
        // }

    }
}
