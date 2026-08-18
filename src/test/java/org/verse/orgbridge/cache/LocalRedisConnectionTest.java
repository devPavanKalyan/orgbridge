package org.verse.orgbridge.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(
        named = "RUN_LOCAL_REDIS_TEST",
        matches = "true"
)
class LocalRedisConnectionTest {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Test
    void localRedisRespondsToReactiveClient() {
        StepVerifier.create(
                        redisTemplate.execute(connection ->
                                        connection.ping()
                                )
                                .single()
                )
                .expectNext("PONG")
                .verifyComplete();
    }
}
