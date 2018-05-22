package notes;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
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
    private TextField newNoteTitle;
    private Stage dialog = new Stage();
    private ObservableList<Note> notesList = FXCollections.observableArrayList();

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
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(window);
        window.setWidth(500);
        BorderPane bPane = new BorderPane();

        listView = new ListView<>(notesList);
        listView.setPrefSize(200, window.getHeight());
        listView.setMaxWidth(200);
        listView.setMinWidth(100);
        listView.setLayoutX(0);
        listView.setLayoutY(0);

        notesList.addAll(notes);

        listView.setItems(notesList);
        bPane.setLeft(listView);

        textArea.setText("This simple note app is built by Karl Hendrik\nLeppmets as a Java project for Java\nTechnologies course in TTÜ IT College.");
        textArea.setMinWidth(200);
        textArea.setPrefWidth(300);
        textArea.setMaxWidth(1000000);
        textArea.setWrapText(true);

        bPane.setRight(textArea);

        HBox hbox = new HBox();
        hbox.setMinWidth(300);
        hbox.setPrefWidth(500);
        hbox.setMaxWidth(1000000);
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #336699;");

        Button newButton = new Button("New note");
        newButton.setPrefSize(100, 20);

        Button saveButton = new Button("Save");
        saveButton.setPrefSize(100, 20);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinSize(10, 1);

        hbox.getChildren().addAll(newButton, spacer, saveButton);
        bPane.setBottom(hbox);

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
        saveButton.setOnAction(e -> {
            try {
                saveCurrentNote();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        newButton.setOnAction(e -> {
            try {
                openTitleModal();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });


        window.setTitle("Notes by Spruur");
        window.setScene(new Scene(bPane, 500, 400));
        window.setMinWidth(400);
        window.setMinHeight(200);
        window.show();
    }

    private void textEdited() {
        if (currentNote != null)
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

    private void openTitleModal() throws Exception {
        VBox dialogVbox = new VBox(20);

        Label label = new Label("Title for new note: ");
        label.setMinWidth(300);
        label.setAlignment(Pos.CENTER);
        newNoteTitle = new TextField();

        Button createNotebutton = new Button("Create new note");
        createNotebutton.setAlignment(Pos.CENTER);

        createNotebutton.setOnAction(e -> {
            try {
                newNote();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        dialogVbox.getChildren().addAll(label, newNoteTitle, createNotebutton);
        Scene dialogScene = new Scene(dialogVbox, 300, 200);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void newNote() throws Exception{
        PreparedStatement stmt = db.prepareStatement("INSERT INTO "+dbPrefix+"notes (title, content, edited) VALUES (?, '', NOW())", Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, newNoteTitle.getText().length() > 0 ? newNoteTitle.getText() : null);
        stmt.executeUpdate();
        ResultSet keys = stmt.getGeneratedKeys();
        int key;
        if (keys.next()) {
            key = keys.getInt(1);
        }
        else {
            throw new Exception("No data inserted!");
        }

        Statement stmt2 = db.createStatement();
        ResultSet rs = stmt2.executeQuery("SELECT * FROM "+dbPrefix+"notes WHERE id="+key);
        if (rs.next()) {
            currentNote = new Note(rs);
            dialog.hide();
            textArea.setText(currentNote.getContent());
            notesList.add(currentNote);
        }
        else
            throw new Exception("No new data to get!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
