package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class WeightedRoundRobinStrategyTest {

    private Map<String, Server> servers;
    private WeightedRoundRobinStrategy strategy;

    @BeforeEach
    public void setUp() {
        servers = new LinkedHashMap<>();
        servers.put("server1", new Server("server1", 2));
        servers.put("server2", new Server("server2", 1));
        servers.put("server3", new Server("server3", 3));
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
