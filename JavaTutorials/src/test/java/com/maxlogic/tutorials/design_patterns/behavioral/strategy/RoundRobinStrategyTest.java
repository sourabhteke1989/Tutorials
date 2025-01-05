package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.LinkedHashMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoundRobinStrategyTest {

    private RoundRobinStrategy roundRobinStrategy;
    private Map<String, Server> servers;

    IntUnaryOperator updateAndGet = idx -> {
        if (idx >= servers.size() - 1) {
            return 0;
        } else {
            return idx + 1;
        }
    };

    @BeforeEach
    public void setUp() {
        servers = new LinkedHashMap<>();
        servers.put("server1", new Server("server1", 2));
        servers.put("server2", new Server("server2", 1));
        servers.put("server3", new Server("server3", 3));
        roundRobinStrategy = new RoundRobinStrategy(servers);
    }

    @Test
    public void testAssignServer() {
        assertEquals("server1", roundRobinStrategy.assignServer().getIp());
        assertEquals("server2", roundRobinStrategy.assignServer().getIp());
        assertEquals("server3", roundRobinStrategy.assignServer().getIp());
        assertEquals("server1", roundRobinStrategy.assignServer().getIp());
    }

}
