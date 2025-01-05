package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class RoundRobinStrategy implements ILoadBalancerStrategy {

  List<Server> serverList;
  AtomicInteger currentIdx = new AtomicInteger(0);
  IntUnaryOperator currentIdxUpdater = idx -> {
    if (idx >= serverList.size() - 1) {
      return 0;
    } else {
      return idx + 1;
    }
  };

  RoundRobinStrategy(List<Server> servers) {
    this.serverList = List.copyOf(servers);
  }

  @Override
  public Server assignServer() {
    return this.serverList.get(currentIdx.getAndUpdate(currentIdxUpdater));
  }
}
