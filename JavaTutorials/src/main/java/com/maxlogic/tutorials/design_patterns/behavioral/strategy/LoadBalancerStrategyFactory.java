package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import java.util.List;

public class LoadBalancerStrategyFactory {

    public static ILoadBalancerStrategy getStrategy(String strategy, List<Server> servers) {
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
