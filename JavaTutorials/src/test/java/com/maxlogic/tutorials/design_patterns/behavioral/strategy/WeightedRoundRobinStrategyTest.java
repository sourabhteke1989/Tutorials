package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class WeightedRoundRobinStrategyTest {

    private List<Server> servers;
    private WeightedRoundRobinStrategy strategy;

    @BeforeEach
    public void setUp() {
        servers = new ArrayList<>();
        servers.add(new Server("server1", 2));
        servers.add(new Server("server2", 1));
        servers.add(new Server("server3", 3));
        strategy = new WeightedRoundRobinStrategy(servers);
    }

    @Test
    public void testAssignServerWithWeights() {
        assertEquals("server1", strategy.assignServer().getIp());
        assertEquals("server1", strategy.assignServer().getIp());
        assertEquals("server2", strategy.assignServer().getIp());
        assertEquals("server3", strategy.assignServer().getIp());
        assertEquals("server3", strategy.assignServer().getIp());
        assertEquals("server3", strategy.assignServer().getIp());
        assertEquals("server1", strategy.assignServer().getIp());
        assertEquals("server1", strategy.assignServer().getIp());
    }
}
