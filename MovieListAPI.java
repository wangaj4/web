import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet(name = "MovieListAPI", urlPatterns = "/api/list")
public class MovieListAPI extends HttpServlet {

    public DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response){

        String entry = request.getParameter("Full");
        String page = request.getParameter("Page");


        JsonObject responseJsonObject = new JsonObject();

        try {


            // Create a new connection to database
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            // create database connection
            Connection dbCon = dataSource.getConnection();

            String[] searchTerms = entry.split("\\s+");
            String fullTextSearch = "";
            for (String term : searchTerms) {
                fullTextSearch += "+" + term + "* ";
            }

            int pageNum = Integer.parseInt(page);
            String query = "SELECT * FROM movies WHERE MATCH(title) AGAINST (? IN BOOLEAN MODE) LIMIT 10 OFFSET ?";
            PreparedStatement statement = dbCon.prepareStatement(query);
            statement.setString(1,fullTextSearch);
            statement.setInt(2,pageNum*10);

            // Log to localhost log
            request.getServletContext().log("query：" + query);


            // Perform the query
            ResultSet movies = statement.executeQuery();
            while(movies.next()){
                //Store year and director
                JsonArray jsonArray = new JsonArray();
                jsonArray.add(Integer.toString(movies.getInt("year")));
                jsonArray.add(movies.getString("director"));
                responseJsonObject.add(movies.getString("title"), jsonArray);
            }
            response.getWriter().write(responseJsonObject.toString());




        }catch (Exception e){
            request.getServletContext().log("Error: ", e);
        }



    }
}
