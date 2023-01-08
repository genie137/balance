package com.avermak.vkube.balance;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.util.ArrayList;

public class Controller implements Runnable {
    @FXML
    private BarChart<String, Integer> nodeHitChartREST;
    @FXML
    private BarChart<String, Integer> nodeHitChartGRPC;
    @FXML
    private LineChart<Integer, Integer> responseTimeChartREST;
    @FXML
    public NumberAxis responseTimeChartRESTTimeAxis;
    @FXML
    private LineChart<Integer, Integer> responseTimeChartGRPC;
    @FXML
    public NumberAxis responseTimeChartGRPCTimeAxis;
    @FXML
    private Button buttonStartStop;
    @FXML
    private Button buttonExit;
    @FXML
    public TextField tfRESTURL;
    @FXML
    public TextField tfGRPCURL;
    @FXML
    public CheckBox cbDemoMode;
    @FXML
    public Label infoLabel;

    private APIRunnerREST runnerREST = null;
    private APIRunnerGRPC runnerGRPC = null;
    private Thread uiUpdater = null;
    NodeHitData nodeHitDataREST = null;
    NodeHitData nodeHitDataGRPC = null;
    ResponseTimeData responseTimeDataREST = null;
    ResponseTimeData responseTimeDataGRPC = null;
    int updateInterval = 1000; // ms

    @FXML
    public void initialize() {
        System.out.println("Initializing Controller");

        System.out.println("Initializing config");
        Config config = Config.getInstance();

        System.out.println("Initializing Charts");
        this.nodeHitDataREST = new NodeHitData();
        XYChart.Series<String, Integer> series = new XYChart.Series<>();
        nodeHitChartREST.setLegendVisible(false);
        nodeHitChartREST.getData().add(series);

        this.nodeHitDataGRPC = new NodeHitData();
        series = new XYChart.Series<>();
        nodeHitChartGRPC.setLegendVisible(false);
        nodeHitChartGRPC.getData().add(series);

        this.responseTimeDataREST = new ResponseTimeData("REST");
        responseTimeChartREST.setLegendVisible(false);
        responseTimeChartREST.setCreateSymbols(false);
        responseTimeChartRESTTimeAxis.setTickLabelFormatter(new TimeAxisLabelConverter());

        this.responseTimeDataGRPC = new ResponseTimeData("gRPC");
        responseTimeChartGRPC.setLegendVisible(false);
        responseTimeChartGRPC.setCreateSymbols(false);
        responseTimeChartGRPCTimeAxis.setTickLabelFormatter(new TimeAxisLabelConverter());

        System.out.println("Starting UI Updater thread");
        this.uiUpdater = new Thread(this);
        this.uiUpdater.setDaemon(true);
        this.uiUpdater.start();

        System.out.println("Initializing UI elements");
        buttonStartStop.setText("Start");
        buttonStartStop.setUserData("stopped");

        tfRESTURL.setText(config.getUrlREST());
        tfGRPCURL.setText(config.getUrlGRPC());
        cbDemoMode.setSelected(config.isDemoMode());

        tfRESTURL.textProperty().addListener((observable, oldValue, newValue) -> {
            Config.getInstance().setUrlREST(newValue);
        });
        tfGRPCURL.textProperty().addListener((observable, oldValue, newValue) -> {
            Config.getInstance().setUrlGRPC(newValue);
        });
        cbDemoMode.setOnAction(event -> {
            Config.getInstance().setDemoMode(cbDemoMode.isSelected());
            tfRESTURL.setDisable(cbDemoMode.isSelected());
            tfGRPCURL.setDisable(cbDemoMode.isSelected());
        });
    }

    public void exit(ActionEvent actionEvent) {
        System.out.println("Exiting");
        Platform.exit();
    }

    public boolean isRunning() {
        return !"stopped".equals(buttonStartStop.getUserData());
    }
    public void startStop(ActionEvent actionEvent) {
        if (isRunning()) {
            System.out.println("Initiating stop sequence");
            if (this.runnerREST != null) {
                System.out.println("Instructing REST runner to stop");
                this.runnerREST.scheduleStop();
                this.runnerREST = null;
            }
            if (this.runnerGRPC != null) {
                System.out.println("Instructing gRPC runner to stop");
                this.runnerGRPC.scheduleStop();
                this.runnerGRPC = null;
            }
            System.out.println("Resetting action button states");
            buttonExit.setDisable(false);
            buttonStartStop.setText("Start");
            buttonStartStop.setUserData("stopped");
            tfRESTURL.setDisable(false);
            tfGRPCURL.setDisable(false);
            cbDemoMode.setDisable(false);
        } else {
            System.out.println("Creating API runner threads");
            this.runnerREST = new APIRunnerREST(this.nodeHitDataREST, this.responseTimeDataREST);
            this.runnerGRPC = new APIRunnerGRPC(this.nodeHitDataGRPC, this.responseTimeDataGRPC);
            System.out.println("Setting action button states");
            buttonExit.setDisable(true);
            buttonStartStop.setText("Stop");
            buttonStartStop.setUserData("running");
            tfRESTURL.setDisable(true);
            tfGRPCURL.setDisable(true);
            cbDemoMode.setDisable(true);
            System.out.println("Waking up UI updater thread");
            this.uiUpdater.interrupt();
            System.out.println("Starting runner threads.");
            this.runnerREST.start();
            this.runnerGRPC.start();
        }
    }

    @Override
    public void run() {
        long lastUIUpdateAt = 0;
        while (true) {
            if (!isRunning()) {
                try {Thread.sleep(5000);} catch (InterruptedException iex) {}
                continue;
            }
            long now = System.currentTimeMillis();
            if (now - lastUIUpdateAt > this.updateInterval) {
                Platform.runLater(() -> {
                    updateHitChart(nodeHitChartREST, nodeHitDataREST);
                    updateHitChart(nodeHitChartGRPC, nodeHitDataGRPC);
                    updateResponseTimeChart(responseTimeChartREST, responseTimeDataREST);
                    updateResponseTimeChart(responseTimeChartGRPC, responseTimeDataGRPC);
                });
                lastUIUpdateAt = now;
            }
            try {Thread.sleep(this.updateInterval);} catch (InterruptedException iex) {}
        }
    }

    private void updateResponseTimeChart(LineChart<Integer, Integer> chart, ResponseTimeData data) {
        ArrayList<String> nodeNamesRendered = new ArrayList<>();
        for (var series : chart.getData()) {
            nodeNamesRendered.add(series.getName());
        }
        String[] nodeNamesAvailable = data.getNodeNames();
        for (String nodeNameAvailable : nodeNamesAvailable) {
            int seriesIndex = nodeNamesRendered.indexOf(nodeNameAvailable);
            XYChart.Series<Integer, Integer> series = null;
            if (seriesIndex == -1) {
                series = new XYChart.Series<>();
                series.setName(nodeNameAvailable);
                chart.getData().add(series);
            } else {
                series = chart.getData().get(seriesIndex);
                String color = ChartNodeColors.getBarColor(nodeNameAvailable);
                if (!"styleSet".equals(series.getNode().getUserData())) {
                    series.getNode().setStyle("-fx-stroke:#"+color+";");
                    // chart.lookup(".default-color0.chart-line-symbol").setStyle("-fx-background-color: #666, white;");
                    series.getNode().setUserData("styleSet");
                }
            }
            var responseTimes = data.getResponseTimes(nodeNameAvailable);
            for (var r : responseTimes) {
                series.getData().add(new XYChart.Data<>(r.dt, r.responseTime));
            }
        }
    }

    private void updateHitChart(BarChart<String, Integer> nodeHitChart, NodeHitData nodeHitData) {
        var series = nodeHitChart.getData().get(0);
        var seriesData = series.getData();
        int dataSize = seriesData.size();

        for (String nodeName : nodeHitData.getNodeNames()) {
            int nodeIndex = -1;
            for (int i=0; i<dataSize; i++) {
                if (seriesData.get(i).getXValue().equals(nodeName)) {
                    nodeIndex = i;
                    break;
                }
            }
            int hits = nodeHitData.getHitsForNode(nodeName);
            if (nodeIndex == -1) {
                var xyData = new XYChart.Data<>(nodeName, hits);
                seriesData.add(xyData);
                xyData.getNode().setStyle("-fx-bar-fill:#"+ChartNodeColors.getBarColor(nodeName)+";");
                displayLabelForBar(xyData);
            } else {
                seriesData.get(nodeIndex).setYValue(hits);
            }
        }
    }

    private void displayLabelForBar(XYChart.Data<String, Integer> data) {
        final Node node = data.getNode();
        final Text valueLabel = new Text(data.getYValue() + "");
        valueLabel.setStroke(Paint.valueOf("#f0f0f0"));
        valueLabel.setStrokeWidth(1);
        valueLabel.setStyle("-fx-font-size:10pt;");
        ((Group)node.getParent()).getChildren().add(valueLabel);
        node.boundsInParentProperty().addListener((ov, oldBounds, bounds) -> {
            valueLabel.setText(data.getYValue() + "");
            valueLabel.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - valueLabel.prefWidth(-1) / 2));
            valueLabel.setLayoutY(Math.round(bounds.getMinY() + valueLabel.prefHeight(-1)));
        });
    }
}