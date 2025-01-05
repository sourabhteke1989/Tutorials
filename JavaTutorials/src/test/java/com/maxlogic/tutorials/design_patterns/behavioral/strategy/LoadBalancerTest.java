package com.maxlogic.tutorials.design_patterns.behavioral.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadBalancerTest {

    private List<Server> servers;
    private LoadBalancer loadBalancer;

    @BeforeEach
    public void setUp() {
        servers = new ArrayList<>();
        servers.add(new Server("server1", 1));
        servers.add(new Server("server2", 2));
        servers.add(new Server("server3", 3));
        loadBalancer = new LoadBalancer(servers, "WEIGHTED_ROUND_ROBIN");
    }

    @Test
    public void testAssignServer() {
        Server server = loadBalancer.assignServer();
        assertNotNull(server);
        assertTrue(servers.stream().anyMatch(s -> s.getIp().equals(server.getIp())));
    }

    @Test
    public void testUpdateLoadBalancer() {
        List<Server> newServers = new ArrayList<>();
        newServers.add(new Server("server4", 4));
        newServers.add(new Server("server5", 5));
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
    public void testRoundRobinStrategyCount() throws InterruptedException {
        loadBalancer.updateLoadBalancer(servers, "ROUND_ROBIN");
        assertEquals("ROUND_ROBIN", loadBalancer.getStrategy());

        Map<String, Integer> serverCount = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 300; i++) {
            executorService.submit(() -> {
                Server server = loadBalancer.assignServer();
                serverCount.compute(server.getIp(), (k, v) -> v == null ? 1 : v + 1);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

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
    public void testWeightedRoundRobinStrategyCount() throws InterruptedException {
        loadBalancer.updateLoadBalancer(servers, "WEIGHTED_ROUND_ROBIN");
        assertEquals("WEIGHTED_ROUND_ROBIN", loadBalancer.getStrategy());

        Map<String, Integer> serverCount = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        CountDownLatch latch = new CountDownLatch(600);
        for (int i = 0; i < 600; i++) {
            executorService.submit(() -> {
                Server server = loadBalancer.assignServer();
                serverCount.compute(server.getIp(), (k, v) -> v == null ? 1 : v + 1);
                latch.countDown();
            });
        }

        latch.await();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(100, serverCount.get("server1"));
        assertEquals(200, serverCount.get("server2"));
        assertEquals(300, serverCount.get("server3"));
    }
}
