package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

  List<Server> servers;
  String strategy;
  ILoadBalancerStrategy loadBalancerStrategy;

  LoadBalancer(List<Server> servers, String strategy) {
    List<Server> serversList = new ArrayList<>(servers);
    ILoadBalancerStrategy loadBalancerStrategy =
        LoadBalancerStrategyFactory.getStrategy(strategy, serversList);
    this.loadBalancerStrategy = loadBalancerStrategy;
    this.servers = serversList;
    this.strategy = strategy;
  }

  public synchronized void updateLoadBalancer(List<Server> servers, String strategy) {
    List<Server> serversList = new ArrayList<>(servers);
    ILoadBalancerStrategy loadBalancerStrategy =
        LoadBalancerStrategyFactory.getStrategy(strategy, serversList);
    this.loadBalancerStrategy = loadBalancerStrategy;
    this.servers = serversList;
    this.strategy = strategy;
  }

  public synchronized List<Server> getServers() {
    return servers;
  }

  public synchronized String getStrategy() {
    return strategy;
  }

  public Server assignServer() {
    return loadBalancerStrategy.assignServer();
  }

}
