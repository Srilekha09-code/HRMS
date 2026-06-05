package com.hrms.project.cacheService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActiveWorkerCacheService {

    private final StringRedisTemplate redis;

    private static final String KEY = "active_workers";

    public void add(Long workerId, String workerName) {
        try {
            redis.opsForHash().put(KEY, workerId.toString(), workerName);
            redis.expire(KEY, Duration.ofHours(16));
        } catch (Exception e) {
            System.out.println("Redis Down → fallback DB");
        }
    }

    public void remove(Long workerId) {
        redis.opsForHash().delete(KEY, workerId.toString());
    }

    public List<Map<String, Object>> getAll() {

        List<Object> values = redis.opsForHash().values(KEY);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object obj : values) {

            String value = obj.toString();

            // assuming stored like: workerName|siteId|siteName|time
            String[] parts = value.split("\\|");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("workerName", parts[0]);
            map.put("siteId", parts[1]);
            map.put("siteName", parts[2]);
            map.put("clockInTime", parts[3]);

            result.add(map);
        }

        return result;
    }


    public void put(Long workerId, String workerName, Long siteId, String siteName, OffsetDateTime clockInTime) {
        try {
            String value = workerName + "|" + siteId + "|" + clockInTime;

            redis.opsForHash().put(KEY, workerId.toString(), value);
            redis.expire(KEY, Duration.ofHours(16));
        } catch (Exception e) {
            System.out.println("Redis Down → fallback DB");
        }
    }



}


