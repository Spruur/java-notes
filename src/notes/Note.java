package notes;

import java.sql.ResultSet;

public class Note {
    private int id;
    private String title;
    private String content;
    private String edited;
    private String password;

    Note(ResultSet resultSet) throws Exception {
        id = resultSet.getInt("id");
        title = resultSet.getString("title");
        content = resultSet.getString("content");
        edited = resultSet.getString("edited");
        password = resultSet.getString("password");
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getEdited() {
        return edited;
    }

    public String getPassword() {
        return password;
    }
}
