package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class WeightedRoundRobinStrategy implements ILoadBalancerStrategy {

  List<Server> serverList;
  AtomicInteger currentIdx = new AtomicInteger(0);
  int maxWeight;
  AtomicInteger currentWeight = new AtomicInteger(1);
  List<Integer> weightLimits = new ArrayList<>();

  IntUnaryOperator weightUpdater = weight -> {
    if (weight == maxWeight) {
      return 1;
    } else {
      return weight + 1;
    }
  };

  IntUnaryOperator currentIdxUpdater = idx -> {
    int weight = currentWeight.getAndUpdate(weightUpdater);
    if (weight >= weightLimits.get(idx)) {
      if (idx >= serverList.size() - 1) {
        return 0;
      } else {
        return idx + 1;
      }
    } else {
      return idx;
    }
  };

  WeightedRoundRobinStrategy(List<Server> servers) {
    this.serverList = List.copyOf(servers);
    initializeWeights();
  }

  private void initializeWeights() {
    int weight = 0;
    for (Server server : this.serverList) {
      weight += server.getWeight();
      weightLimits.add(weight);
    }
    this.maxWeight = weight;
  }

  @Override
  public Server assignServer() {
    int idx;
    synchronized (this) {
      idx = currentIdx.getAndUpdate(currentIdxUpdater);
    }
    return this.serverList.get(idx);
  }
}
