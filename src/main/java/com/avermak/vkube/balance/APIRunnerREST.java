package com.avermak.vkube.balance;

import com.google.gson.Gson;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.HashMap;

public class APIRunnerREST extends APIRunner {
    public APIRunnerREST(Config cfg, NodeHitData hdata, ResponseTimeData rdata) {
        super(cfg, hdata, rdata);
        if (cfg.usesTLS()) {
            bypassTLSCertCheck();
        }
    }

    @Override
    public void call() throws Exception {
        InputStream istream = null;
        try {
            String urlStr = "http" + (this.config.usesTLS() ? "s" : "") + "://" + this.config.getHost() + ":" + this.config.getPort() + "/hello/api?name=lbtester";
            URL url = new URL(urlStr);
            long start = System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            istream = conn.getInputStream();
            String res = readStreamText(istream);
            long end = System.currentTimeMillis();
            try {
                Gson gson = new Gson();
                HashMap reply = gson.fromJson(res, HashMap.class);
                String nodeName = (String) reply.get("nodeName");
                recordHitData(nodeName, (int)(end-start));
            } catch (Exception ex) {
                System.out.println("Error processing json response. Dumping raw response text.\n"+res);
                throw ex;
            }
        } finally {
            if (istream != null) {try {istream.close();} catch (Exception ex) {}}
        }
    }
    public static void bypassTLSCertCheck() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception ex) {
            System.out.println("Error bypassing TLS cert check. TLS may not work. " + ex);
            ex.printStackTrace();
        }
    }
}
