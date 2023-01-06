module com.avermak.vkube.balance {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.google.gson;
    requires vkube.hello.grpc;
    requires grpclib;

    opens com.avermak.vkube.balance to javafx.fxml;
    exports com.avermak.vkube.balance;
}