package com.avermak.vkube.balance;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;

import java.util.ArrayList;

public class Controller implements Runnable {
    @FXML
    private BarChart<String, Integer> nodeHitChartREST;
    @FXML
    private BarChart<String, Integer> nodeHitChartGRPC;
    @FXML
    private LineChart<Integer, Integer> responseTimeChartREST;
    @FXML
    private LineChart<Integer, Integer> responseTimeChartGRPC;
    @FXML
    private Button buttonStartStop;
    @FXML
    private Button buttonSave;
    @FXML
    private Button buttonExit;

    private APIRunnerREST runnerREST = null;
    private APIRunnerGRPC runnerGRPC = null;
    private Thread uiUpdater = null;
    NodeHitData nodeHitDataREST = null;
    NodeHitData nodeHitDataGRPC = null;
    ResponseTimeData responseTimeDataREST = null;
    ResponseTimeData responseTimeDataGRPC = null;
    int updateInterval = 1000; // ms
    Config config = null;

    @FXML
    public void initialize() {
        System.out.println("Initializing Controller");

        this.config = new Config("192.168.1.80", 443, true, 500);
        this.config.setWarmupCount(3);

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

        this.responseTimeDataGRPC = new ResponseTimeData("gRPC");
        responseTimeChartGRPC.setLegendVisible(false);
        responseTimeChartGRPC.setCreateSymbols(false);

        System.out.println("Starting UI Updater thread");
        this.uiUpdater = new Thread(this);
        this.uiUpdater.setDaemon(true);
        this.uiUpdater.start();

        System.out.println("Initializing action buttons");
        buttonStartStop.setText("Start");
        buttonStartStop.setUserData("stopped");

        System.out.println("Controller initialization complete.");
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
            buttonSave.setDisable(false);
            buttonStartStop.setText("Start");
            buttonStartStop.setUserData("stopped");
        } else {
            System.out.println("Creating API runner threads");
            this.runnerREST = new APIRunnerREST(this.config, this.nodeHitDataREST, this.responseTimeDataREST);
            this.runnerGRPC = new APIRunnerGRPC(this.config, this.nodeHitDataGRPC, this.responseTimeDataGRPC);
            System.out.println("Setting action button states");
            buttonSave.setDisable(true);
            buttonExit.setDisable(true);
            buttonStartStop.setText("Stop");
            buttonStartStop.setUserData("running");
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
            } else {
                seriesData.get(nodeIndex).setYValue(hits);
            }
        }
    }
}