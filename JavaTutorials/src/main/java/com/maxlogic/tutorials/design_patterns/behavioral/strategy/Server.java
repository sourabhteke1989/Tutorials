package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

public class Server {
    private String ip;
    private int weight;

    public static Server of(String ip, int weight) {
        return new Server(ip, weight);
    }

    public Server(String ip, int weight) {
        this.ip = ip;
        this.weight = weight;
    }

    public String getIp() {
        return ip;
    }

    public int getWeight() {
        return weight;
    }

}
