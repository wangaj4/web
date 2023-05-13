//SAX XML Parser to add new movie entries to a mysql database

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class SAXParserExample extends DefaultHandler {

    Connection connection;
    private PreparedStatement preparedStatement;
    private boolean useReleased;
    List<Film> Films;

    private String tempVal;

    //to maintain context
    private Film tempFilm;

    public SAXParserExample() {
        Films = new ArrayList<Film>();
    }

    public void runExample() {
        try{
            String loginUser = "mytestuser";
            String loginPasswd = "My6$Password";
            String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            // create database connection
            connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
            String insertQuery = "INSERT INTO movies2 (id, title, year, director) VALUES (?, ?, ?, ?)";
            preparedStatement = connection.prepareStatement(insertQuery);

            parseDocument();

            preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();
        }catch  (Exception e){
            System.out.println("error in run " + e);
        }
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
            sp.parse("mains.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }


    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";
        if (qName.equalsIgnoreCase("film")) {
            //create a new instance of a film
            tempFilm = new Film();
            useReleased = false;
        }else if (qName.equalsIgnoreCase("released")) {
            useReleased = true;
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("film")) {
            //add it to the list
            Films.add(tempFilm);
            try{
                preparedStatement.setString(1, tempFilm.getID());
                preparedStatement.setString(2, tempFilm.getTitle());
                preparedStatement.setInt(3,tempFilm.getYear());
                preparedStatement.setString(4, tempFilm.getDirector());
                preparedStatement.addBatch();

            }catch (Exception e){
                System.out.println("error in endElement");
            }


        } else if (qName.equalsIgnoreCase("t")) {
            tempFilm.setTitle(tempVal);
        } else if (qName.equalsIgnoreCase("fid")) {
            tempFilm.setID(tempVal);
        } else if (qName.equalsIgnoreCase("year") && !useReleased) {
            tempFilm.setYear(Integer.parseInt(tempVal));
        } else if (qName.equalsIgnoreCase("released") && useReleased) {
            tempFilm.setYear(Integer.parseInt(tempVal));
        }else if (qName.equalsIgnoreCase("dirn")) {
            tempFilm.setDirector(tempVal);
        }else if (qName.equalsIgnoreCase("cat")) {
            tempFilm.addGenre(tempVal);
        }

    }

    public static void main(String[] args) {
        SAXParserExample spe = new SAXParserExample();
        spe.runExample();
    }

}
