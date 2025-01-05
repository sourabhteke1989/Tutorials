package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadBalancerTest {

    private Map<String, Server> servers;
    private LoadBalancer loadBalancer;

    @BeforeEach
    public void setUp() {
        servers = new LinkedHashMap<>();
        servers.put("server1", new Server("server1", 1));
        servers.put("server2", new Server("server2", 2));
        servers.put("server3", new Server("server3", 3));
        loadBalancer = new LoadBalancer(servers, "WEIGHTED_ROUND_ROBIN");
    }

    @Test
    public void testAssignServer() {
        Server server = loadBalancer.assignServer();
        assertNotNull(server);
        assertTrue(servers.containsKey(server.getIp()));
    }

    @Test
    public void testUpdateLoadBalancer() {
        Map<String, Server> newServers = new HashMap<>();
        newServers.put("server4", new Server("server4", 4));
        newServers.put("server5", new Server("server5", 5));
        loadBalancer.updateLoadBalancer(newServers, "ROUND_ROBIN");

        assertEquals("ROUND_ROBIN", loadBalancer.getStrategy());
        assertEquals(newServers, loadBalancer.getServers());
    }

    @Test
    public void testGetServers() {
        assertEquals(servers, loadBalancer.getServers());
    }

    @Test
    public void testGetStrategy() {
        assertEquals("WEIGHTED_ROUND_ROBIN", loadBalancer.getStrategy());
    }

    @Test
    public void testRoundRobinStrategySequene() {
        loadBalancer.updateLoadBalancer(servers, "ROUND_ROBIN");
        assertEquals("ROUND_ROBIN", loadBalancer.getStrategy());
        assertEquals("server1", loadBalancer.assignServer().getIp());
        assertEquals("server2", loadBalancer.assignServer().getIp());
        assertEquals("server3", loadBalancer.assignServer().getIp());
        assertEquals("server1", loadBalancer.assignServer().getIp());
    }

    @Test
    public void testRoundRobinStrategyCount() {
        loadBalancer.updateLoadBalancer(servers, "ROUND_ROBIN");
        assertEquals("ROUND_ROBIN", loadBalancer.getStrategy());

        Map<String, Integer> serverCount = new HashMap<>();
        for (int i = 0; i < 300; i++) {
            Server server = loadBalancer.assignServer();
            serverCount.put(server.getIp(), serverCount.getOrDefault(server.getIp(), 0) + 1);
        }

        assertEquals(100, serverCount.get("server1"));
        assertEquals(100, serverCount.get("server2"));
        assertEquals(100, serverCount.get("server3"));
    }

    @Test
    public void testWeightedRoundRobinStrategySequence() {
        loadBalancer.updateLoadBalancer(servers, "WEIGHTED_ROUND_ROBIN");
        assertEquals("WEIGHTED_ROUND_ROBIN", loadBalancer.getStrategy());
        assertEquals("server1", loadBalancer.assignServer().getIp());
        assertEquals("server2", loadBalancer.assignServer().getIp());
        assertEquals("server2", loadBalancer.assignServer().getIp());
        assertEquals("server3", loadBalancer.assignServer().getIp());
        assertEquals("server3", loadBalancer.assignServer().getIp());
        assertEquals("server3", loadBalancer.assignServer().getIp());
        assertEquals("server1", loadBalancer.assignServer().getIp());
        assertEquals("server2", loadBalancer.assignServer().getIp());
        assertEquals("server2", loadBalancer.assignServer().getIp());
    }

    @Test
    public void testWeightedRoundRobinStrategyCount() {
        loadBalancer.updateLoadBalancer(servers, "WEIGHTED_ROUND_ROBIN");
        assertEquals("WEIGHTED_ROUND_ROBIN", loadBalancer.getStrategy());

        Map<String, Integer> serverCount = new HashMap<>();
        for (int i = 0; i < 600; i++) {
            Server server = loadBalancer.assignServer();
            serverCount.put(server.getIp(), serverCount.getOrDefault(server.getIp(), 0) + 1);
        }

        assertTrue(serverCount.get("server1") < serverCount.get("server2"));
        assertTrue(serverCount.get("server2") < serverCount.get("server3"));
        assertEquals(100, serverCount.get("server1"));
        assertEquals(200, serverCount.get("server2"));
        assertEquals(300, serverCount.get("server3"));
    }
}
