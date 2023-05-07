import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import org.jasypt.util.password.StrongPasswordEncryptor;


@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    public DataSource dataSource;


    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {


        //Check recaptcha
        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");



        String username = request.getParameter("username");
        String password = request.getParameter("password");

        /* This example only allows username/password to be test/test
        /  in the real project, you should talk to the database to verify username/password
        */
        response.setContentType("text/html");    // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();

        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        try{


            // Create a new connection to database
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            // create database connection
            Connection dbCon = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);

            // Declare a new statement
            // Generate a SQL query
            //String query = String.format("SELECT * from customers where email = '%s'", username);
            String query = "SELECT * from customers where email = ?";
            PreparedStatement statement = dbCon.prepareStatement(query);
            statement.setString(1,username);

            // Log to localhost log
            request.getServletContext().log("query：" + query);



            // Perform the query
            ResultSet usernames = statement.executeQuery();

            Boolean existinguser = false;


            String correspondingPassword = "";
            Boolean success = false;
            while (usernames.next()){
                existinguser = true;
                correspondingPassword = usernames.getString("password");
                success = new StrongPasswordEncryptor().checkPassword(password, correspondingPassword);

            }
            if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Input Captcha");
            }

            else if (existinguser && success) {
                // Login success:

                // set this user into the session
                request.getSession().setAttribute("user", new User(username));

                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");


            } else {
                // Login fail
                responseJsonObject.addProperty("status", "fail");
                // Log to localhost log
                request.getServletContext().log("Login failed");
                // sample error messages. in practice, it is not a good idea to tell user which one is incorrect/not exist.
                if (!existinguser) {
                    responseJsonObject.addProperty("message", "user " + username + " doesn't exist");
                } else {
                    responseJsonObject.addProperty("message", "incorrect password");
                }
            }
            response.getWriter().write(responseJsonObject.toString());
        }
        catch(Exception e){
            request.getServletContext().log("Error: ", e);

            // Output Error Message to html
            out.println(String.format("<html><head><title>CAPTCHA: Error</title></head>\n<body><p>SQL error in doGet: %s</p></body></html>", e.getMessage()));
            return;
        }



    }
}