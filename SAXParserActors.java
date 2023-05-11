import org.apache.commons.dbcp2.BasicDataSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.*;


public class SAXParserActors extends DefaultHandler {

    Connection connection;
    private BasicDataSource dataSource;
    private PreparedStatement preparedStatement;

    private String tempVal;
    private String tempName = "";
    private int birthYear = 0;
    private int count = 0;

    //to maintain context

    private String lastID;

    private void getLatestID(){
        String query = "SELECT id FROM stars order by id desc limit 1";
        try{
            PreparedStatement IdStatement = connection.prepareStatement(query);
            ResultSet result = IdStatement.executeQuery();
            while (result.next()){
                lastID = result.getString("id");
                System.out.println(lastID);
            }

        }catch(Exception e){
            System.out.println("Error in getLatestID: " + e);
        }
    }


    private void incrementID(){
        //Converts lastID into the next ID that comes after for new ID
        int digits = Integer.parseInt(lastID.substring(lastID.length() - 7));
        digits += 1;
        lastID = lastID.substring(0,2) + Integer.toString(digits);

    }

    public void runExample() {
        try{


            dataSource = new BasicDataSource();
            dataSource.setUrl("jdbc:mysql://localhost:3306/moviedb");
            dataSource.setUsername("mytestuser");
            dataSource.setPassword("My6$Password");
            // create database connection
            connection = dataSource.getConnection();
            getLatestID();

            connection.setAutoCommit(false);

            String query = "INSERT INTO stars2(id, name, birthYear) VALUES(?,?,?)";
            preparedStatement = connection.prepareStatement(query);

            parseDocument();

            preparedStatement.executeBatch();


            connection.commit();
            preparedStatement.close();

        }catch  (Exception e){
            System.out.println("error in run " + e);
        }
        printData();
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
            sp.parse("actors63.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Iterate through the list and print
     * the contents
     */
    private void printData() {
            System.out.println(count);

    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        tempVal = "";

        if (qName.equalsIgnoreCase("actor")) {
            tempName = "";
            birthYear = 0;

        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("actor")) {
            count+=1;
            //add it to the list
            try{
                //prepare id
                incrementID();

                preparedStatement.setString(1,lastID);
                preparedStatement.setString(2, tempName);
                if(birthYear != 0) preparedStatement.setInt(3,birthYear);
                else preparedStatement.setNull(3, Types.INTEGER);
                preparedStatement.addBatch();

            }catch (Exception e){
                System.out.println("error in endElement: " + e);
            }


        } else if (qName.equalsIgnoreCase("stagename")) {
            tempName = tempVal;
        }else if (qName.equalsIgnoreCase("dob")) {
            try{
                if(!tempVal.equals("")) birthYear = Integer.parseInt(tempVal);
            }catch(Exception e){
                birthYear = 0;
            }

        }

    }

    public static void main(String[] args) {
        SAXParserActors spe = new SAXParserActors();
        spe.runExample();
    }

}
