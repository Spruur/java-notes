package notes;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
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
    private ArrayList<String> neededTables = new ArrayList<>(Arrays.asList(dbPrefix+"notes"));
    private TextArea textArea = new TextArea();
    private ListView<Note> listView;
    private Note currentNote;

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        connectDb();
        checkDatabase();

        getNotes();
        buildView();
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
        stmt.executeUpdate("CREATE TABLE `"+dbName+"`.`"+dbPrefix+"notes` ( `id` INT(11) NOT NULL AUTO_INCREMENT , `title` VARCHAR(254) NULL , `content` TEXT NOT NULL , `edited` DATETIME NOT NULL , `password` VARCHAR(32) NULL DEFAULT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;");
    }

    private void getNotes() throws Exception {
        Statement stmt = db.createStatement();
        ResultSet resultSet = stmt.executeQuery("SELECT * FROM "+dbPrefix+"notes");

        while (resultSet.next()) {
            notes.add(new Note(resultSet));
        }
    }

    private void buildView() throws Exception {
        window.setWidth(500);
        BorderPane bPane = new BorderPane();

        ObservableList<Note> data = FXCollections.observableArrayList();

        listView = new ListView<>(data);
        listView.setPrefSize(200, window.getHeight());
        listView.setMaxWidth(200);
        listView.setMinWidth(100);
        listView.setLayoutX(0);
        listView.setLayoutY(0);

        data.addAll(notes);

        listView.setItems(data);
        bPane.setLeft(listView);

        textArea.setText("Testilause");
        textArea.setMinWidth(200);
        textArea.setPrefWidth(300);
        textArea.setMaxWidth(1000000);
        textArea.setWrapText(true);

        bPane.setRight(textArea);

        // Listeners
        bPane.widthProperty().addListener((ChangeListener) -> textArea.setPrefWidth(window.getWidth() - listView.getWidth()));
        listView.getSelectionModel().selectedItemProperty().addListener((ObservableValue) -> {
            try {
                setCurrentNote(listView.getSelectionModel().getSelectedItem());
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {textArea.setPrefWidth(window.getWidth() - listView.getWidth()); textEdited(); });



        window.setTitle("Notes by Spruur");
        window.setScene(new Scene(bPane, 500, 400));
        window.setMinWidth(300);
        window.setMinHeight(200);
        window.show();
    }

    private void textEdited() {
        currentNote.updateContent(textArea.getText());
    }

    private void setCurrentNote(Note note) throws Exception {
        saveCurrentNote();
        textArea.setPrefWidth(window.getWidth() - listView.getWidth());  // Just in case for screen size fixing.


        currentNote = note;
        textArea.setText(currentNote.getContent());
    }

    private void saveCurrentNote() throws Exception {
        if (currentNote != null) {
            PreparedStatement stmt = db.prepareStatement("UPDATE "+dbPrefix+"notes SET content=?, edited=NOW() WHERE id=?");
            stmt.setString(1, currentNote.getContent());
            stmt.setInt(2, currentNote.getId());
            stmt.executeUpdate();
        }
    }




    public static void main(String[] args) {
        launch(args);
    }
}
