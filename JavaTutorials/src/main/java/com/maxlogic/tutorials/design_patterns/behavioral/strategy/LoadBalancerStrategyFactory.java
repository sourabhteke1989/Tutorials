package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.Map;

public class LoadBalancerStrategyFactory {

    public static ILoadBalancerStrategy getStrategy(String strategy, Map<String, Server> servers) {
        if (strategy == null) {
            return null;
        }
        switch (strategy) {
            case "ROUND_ROBIN":
                return new RoundRobinStrategy(servers);
            case "WEIGHTED_ROUND_ROBIN":
                return new WeightedRoundRobinStrategy(servers);
        }
        return null;
    }

}
