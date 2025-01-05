package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.LinkedHashMap;
import java.util.Map;

public class StrategyPatternMain {

  /**
   * Strategy Pattern (Policy Pattern): Define a family of algorithms, encapsulate each one, and
   * make them interchangeable. Strategy lets the algorithm vary independently from clients that use
   * it.
   *
   * Entities : Strategy - common interface for all strategies, declares algorithm methods.
   * ConcreteStrategy - implements algorithm using Strategy interface. Context - is configured with
   * ConcreteStrategy object, has ConcreteStrategy object as instance property, may define interface
   * that lets Strategy access its data.
   *
   * Example : Load balancer different strategy implementations. Strategy - ILoadBalancerStrategy
   * interface, declares assignServer() algorithm method. ConcreteStrategy - RoundRobinStrategy,
   * WeightRoundRobinStrategy classes. Context - LoadBalancer class, maintains reference to strategy
   * object. Also contains servers related all information.
   */

  public static void main(String[] args) {
    // Load balancer with Round Robin strategy
    Map<String, Server> servers = new LinkedHashMap<>();
    servers.put("server1", new Server("server1", 2));
    servers.put("server2", new Server("server2", 1));
    servers.put("server3", new Server("server3", 3));
    LoadBalancer loadBalancer = new LoadBalancer(servers, "ROUND_ROBIN");
    for (int i = 0; i < 10; i++) {
      System.out.println("Got Server with IP :" + loadBalancer.assignServer().getIp());
    }
  }

}
