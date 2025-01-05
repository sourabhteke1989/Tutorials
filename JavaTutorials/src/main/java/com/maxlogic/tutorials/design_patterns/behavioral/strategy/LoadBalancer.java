package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoadBalancer {

  Map<String, Server> servers;
  String strategy;
  ILoadBalancerStrategy loadBalancerStrategy;

  LoadBalancer(Map<String, Server> servers, String strategy) {
    Map<String, Server> serversMap = new LinkedHashMap<>(servers);
    ILoadBalancerStrategy loadBalancerStrategy =
        LoadBalancerStrategyFactory.getStrategy(strategy, serversMap);
    this.loadBalancerStrategy = loadBalancerStrategy;
    this.servers = serversMap;
    this.strategy = strategy;
  }

  public synchronized void updateLoadBalancer(Map<String, Server> servers, String strategy) {
    Map<String, Server> serversMap = new LinkedHashMap<>(servers);
    ILoadBalancerStrategy loadBalancerStrategy =
        LoadBalancerStrategyFactory.getStrategy(strategy, serversMap);
    this.loadBalancerStrategy = loadBalancerStrategy;
    this.servers = serversMap;
    this.strategy = strategy;
  }

  public synchronized Map<String, Server> getServers() {
    return servers;
  }

  public synchronized String getStrategy() {
    return strategy;
  }

  public Server assignServer() {
    return loadBalancerStrategy.assignServer();
  }

}
