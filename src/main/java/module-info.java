module com.example.demo2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;

    // Open packages for FXML reflection and JavaFX property access
    opens com.example.demo2 to javafx.fxml;
    opens com.example.demo2.gui to javafx.fxml;
    opens com.example.demo2.entities to javafx.base;

    // Export base package (others are internal)
    exports com.example.demo2;
}