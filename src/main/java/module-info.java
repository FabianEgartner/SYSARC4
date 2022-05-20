module billard.main {
    requires javafx.controls;
    requires java.rmi;
    requires java.desktop;
    requires org.dyn4j;

    exports at.fhv.sysarch.lab4;

    opens at.fhv.sysarch.lab4 to javafx.base;
    //opens at.fhv.sysarch.lab4 to javafx.base, javafx.fxml;
}