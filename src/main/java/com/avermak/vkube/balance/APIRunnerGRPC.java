package com.avermak.vkube.balance;

import com.avermak.vkube.api.hello.HelloReply;
import com.avermak.vkube.api.hello.HelloRequest;
import com.avermak.vkube.api.hello.HelloVKubeServiceGrpc;
import io.grpc.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URL;

public class APIRunnerGRPC extends APIRunner {
    volatile HelloVKubeServiceGrpc.HelloVKubeServiceBlockingStub stub = null;

    public APIRunnerGRPC(NodeHitData hitData, ResponseTimeData resData) {
        super(hitData, resData);
    }

    @Override
    public void call() throws Exception {
        if (Config.getInstance().isDemoMode()) {
            recordHitData("Node0" + random(1, 3), random(10, 50));
            return;
        }
        long start = System.currentTimeMillis();
        HelloReply reply = getStub().sayHello(HelloRequest.newBuilder().setName("grpc-world").build());
        long end = System.currentTimeMillis();
        String nodeName = reply.getNodeName();
        recordHitData(nodeName, (int)(end-start));
    }
    private HelloVKubeServiceGrpc.HelloVKubeServiceBlockingStub getStub() throws Exception {
        if (this.stub == null) {
            synchronized (this) {
                if (this.stub == null) {
                    String urlStr = Config.getInstance().getUrlGRPC();
                    boolean tls = false;
                    if (urlStr.toLowerCase().startsWith("grpc://")) {
                        urlStr = "http://" + urlStr.substring(7);
                    }
                    if (urlStr.toLowerCase().startsWith("grpcs://")) {
                        urlStr = "https://" + urlStr.substring(8);
                        tls = true;
                    }
                    URL url = new URL(urlStr);
                    String host = url.getHost();
                    int port = url.getPort();
                    ManagedChannel channel;
                    if (tls) {
                        System.out.println("Using dummy TLS credentials to bypass self-signed cert");
                        ChannelCredentials credentials = TlsChannelCredentials.newBuilder().trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers()).build();
                        System.out.println("Creating secure channel at " + host + ":" + port);
                        channel = Grpc.newChannelBuilderForAddress(host, port, credentials).build();
                    } else {
                        System.out.println("Creating non-secure channel at " + host + ":" + port);
                        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
                    }
                    System.out.println("Creating new blocking stub");
                    this.stub = HelloVKubeServiceGrpc.newBlockingStub(channel);
                }
            }
        }
        return this.stub;
    }
}
