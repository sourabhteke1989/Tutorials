package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.function.IntUnaryOperator;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoundRobinStrategyTest {

    private RoundRobinStrategy roundRobinStrategy;
    private List<Server> servers;

    IntUnaryOperator updateAndGet = idx -> {
        if (idx >= servers.size() - 1) {
            return 0;
        } else {
            return idx + 1;
        }
    };

    @BeforeEach
    public void setUp() {
        servers = new ArrayList<>();
        servers.add(new Server("server1", 2));
        servers.add(new Server("server2", 1));
        servers.add(new Server("server3", 3));
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
