package notes;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Main extends Application {
    private Stage window;

    private Connection db;
    private String dbName = "notes";        // CHANGE DATABASE NAME IF NEEDED
    private String dbHost = "172.20.8.102"; // CHANGE HOSTNAME. THIS SHOULD BE IN MOST CASES localhost
    private int dbPort = 3306;              // Change port, if you use different one for your database
    private String dbUser = "notes";        // CHANGE USER IF YOU ARE USING DIFFERENT ONE
    private String dbPass = "mypass";       // CHANGE PASSWORD OF THE USER
    private String dbPrefix = "notes_by_spruur_";


    private ArrayList<Note> notes = new ArrayList<>();
    private ArrayList<String> neededTables = new ArrayList<>(Arrays.asList(dbPrefix+"sdf", dbPrefix+"notes"));

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        connectDb();
        checkDatabase();

        Group gameGroup = new Group();

        for (int i=0; i<10; i++) {
            for (int j=0; j<10; j++) {
                Rectangle rect = new Rectangle(i*40, j*40, 40, 40);
                rect.setFill(Color.AQUA);
                rect.setStroke(Color.BLACK);

                gameGroup.getChildren().add(rect);
            }
        }

        window.setTitle("Ship destroyer - Login");
        window.setScene(new Scene(gameGroup, 400, 400));
        window.show();

        getNotes();
        //buildView();
    }

    private boolean connectDb() throws Exception {
        Properties info = new Properties();
        info.put("user", dbUser);
        info.put("password", dbPass);
        db = DriverManager.getConnection("jdbc:mysql://"+dbHost+":"+dbPort+"/"+dbName, info);
        return true;
    }

    private void checkDatabase() throws Exception{
        if (!hasTables()) {
            buildTables();
        }
    }

    private boolean hasTables() throws Exception {
        ResultSet res = db.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
        ArrayList<String> existingTables = new ArrayList<>();
        while (res.next()) {
            String tblName = res.getString("TABLE_NAME");
            if (neededTables.contains(tblName))
                existingTables.add(tblName);
        }

        neededTables.sort(String::compareToIgnoreCase);
        existingTables.sort(String::compareToIgnoreCase);


        return neededTables.equals(existingTables);
    }

    private void buildTables() throws Exception {
        Statement stmt = db.createStatement();
        stmt.executeUpdate("CREATE TABLE `"+dbName+"`.`notes` ( `id` INT(11) NOT NULL AUTO_INCREMENT , `title` VARCHAR(254) NULL , `content` TEXT NOT NULL , `edited` DATETIME NOT NULL , `password` VARCHAR(32) NULL DEFAULT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
    }

    private void getNotes() throws Exception {
        Statement stmt = db.createStatement();
        ResultSet resultSet = stmt.executeQuery("SELECT * FROM "+dbPrefix+"notes");

        while (resultSet.next()) {
            notes.add(new Note(resultSet));
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}