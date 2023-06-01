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

        
        response.setContentType("text/html");   

        PrintWriter out = response.getWriter();
        JsonObject responseJsonObject = new JsonObject();


        try{

            //Create connection
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection dbCon = dataSource.getConnection();

            //Prepare query
            String query = "SELECT * from customers where email = ?";
            PreparedStatement statement = dbCon.prepareStatement(query);
            statement.setString(1,username);

            request.getServletContext().log("query：" + query);



            ResultSet usernames = statement.executeQuery();

            Boolean existinguser = false;


            String correspondingPassword = "";
            Boolean success = false;
            
            
            while (usernames.next()){
                //Check if username and password exist as a pair
                existinguser = true;
                correspondingPassword = usernames.getString("password");
                success = new StrongPasswordEncryptor().checkPassword(password, correspondingPassword);

            }
            if (gRecaptchaResponse == null || gRecaptchaResponse.isEmpty()) {
                //If recaptcha was not used, login fail
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
                // error messages
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
