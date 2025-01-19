module de.qiasoft.qiamdreader {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires java.logging;

    opens de.qiasoft.qiamdreader to javafx.fxml;

    exports de.qiasoft.qiamdreader;
}