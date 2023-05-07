import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;


@WebServlet("/Order")
public class OrderPage extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {



        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        out.println("<html>");

        try {


            out.println("<body>");

            //Check if payment details are valid.
            String first = request.getParameter("first");
            String last = request.getParameter("last");
            String num = request.getParameter("num");
            String dateString = request.getParameter("date");

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            java.util.Date date = dateFormat.parse(dateString);
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());

            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            String query = "SELECT * FROM creditcards left join customers on creditcards.id = customers.ccId WHERE creditcards.firstName = ? AND creditcards.lastName = ? " +
                    "AND creditcards.id = ? AND expiration = ?";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1,first);
            statement.setString(2,last);
            statement.setString(3,num);
            statement.setDate(4,sqlDate);
            ResultSet resultSet = statement.executeQuery();
            

            Boolean notEmpty = false;
            String customerID = "";
            while (resultSet.next()){
                notEmpty = true;
                customerID = resultSet.getString("customers.id");

            }

            if (notEmpty){
                out.println("<h1>Order Success!</h1>");
                HttpSession session = request.getSession();
                CartList cart = (CartList) session.getAttribute("cart");
                Calendar calendar = Calendar.getInstance();
                Date currentDate = calendar.getTime();
                currentDate = new java.sql.Date(currentDate.getTime());

                {


                    //Insert into sales database
                    for (MovieInCart item : cart.getCart()){

                        query = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (" + customerID + ", '" + item.getId() + "', '" + currentDate + "')";
                        statement = connection.prepareStatement(query);
                        statement.executeUpdate(query);
                    }

                    out.println("<h2>Confirmation:</h2>");
                    out.println("<table>");
                    out.println("<thead><tr><th>Item Name</th><th>Price per Movie</th><th>Quantity</th></tr></thead>");
                    out.println("<tbody>");
                    int totalPrice = 0;
                    for (MovieInCart item : cart.getCart()) {
                        out.println("<tr><td>" + item.getName() + "</td><td>" + item.getPrice() + "</td><td>" + item.getCount() + "</td><td>");

                        out.println("</td></tr>");
                        totalPrice += item.getPrice() * item.getCount();
                    }
                    out.println("<tr><td>Total Price:</td><td>" + totalPrice + "</td></tr>");
                    out.println("</tbody>");
                    out.println("</table>");

                    out.println("<p><a href=\"MovieList?reset=true\">" + "Back to Movie Search" + "</a></p>");
                }
            }
            //If no:

            else{
                out.println("<h1>Payment Information is Invalid</h1>");
                out.println("<a href=\"Payment\">" + "Try Again" + "</a>");
            }
            out.println("</body>");





        } catch (Exception e) {

            request.getServletContext().log("Error: ", e);

            out.println("<body>");
            out.println("<h1>Payment Information is Invalid</h1>");
            out.println("<a href=\"Payment\">" + "Try Again" + "</a>");
            out.print("</body>");
        }

        out.println("</html>");
        out.close();

    }




}
