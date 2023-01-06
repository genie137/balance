package com.avermak.vkube.balance;

import com.avermak.vkube.api.hello.HelloReply;
import com.avermak.vkube.api.hello.HelloRequest;
import com.avermak.vkube.api.hello.HelloVKubeServiceGrpc;
import io.grpc.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public class APIRunnerGRPC extends APIRunner {
    volatile HelloVKubeServiceGrpc.HelloVKubeServiceBlockingStub stub = null;

    public APIRunnerGRPC(Config cfg, NodeHitData hitData, ResponseTimeData resData, int thinkTime) {
        super(cfg, hitData, resData, thinkTime);
    }

    @Override
    public void call() {
        long start = System.currentTimeMillis();
        HelloReply reply = getStub().sayHello(HelloRequest.newBuilder().setName("grpc-world").build());
        long end = System.currentTimeMillis();
        String nodeName = reply.getNodeName();
        recordHitData(nodeName, (int)(end-start));
    }
    private HelloVKubeServiceGrpc.HelloVKubeServiceBlockingStub getStub() {
        if (this.stub == null) {
            synchronized (this) {
                if (this.stub == null) {
                    String ip = this.config.getHost();
                    int port = this.config.getPort();
                    ManagedChannel channel;
                    if (this.config.usesTLS()) {
                        System.out.println("Using dummy TLS credentials to bypass self-signed cert");
                        ChannelCredentials credentials = TlsChannelCredentials.newBuilder().trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers()).build();
                        System.out.println("Creating secure channel at " + ip + ":" + port);
                        channel = Grpc.newChannelBuilderForAddress(ip, port, credentials).build();
                    } else {
                        System.out.println("Creating non-secure channel at " + ip + ":" + port);
                        channel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext().build();
                    }
                    System.out.println("Creating new blocking stub");
                    this.stub = HelloVKubeServiceGrpc.newBlockingStub(channel);
                }
            }
        }
        return this.stub;
    }
}
