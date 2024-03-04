package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.response.WeatherVO;
import com.example.service.WeatherService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class WeatherServiceImpl implements WeatherService {
    @Resource
    RestTemplate restTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Value("${spring.weather.key}")
    String key;

    /**
     * 获取天气信息
     *
     * @param longitude
     * @param latitude
     * @return
     */
    public WeatherVO fetchWeather(double longitude, double latitude) {
        return fetchFromCache(longitude, latitude);
    }

    private WeatherVO fetchFromCache(double longitude, double latitude) {
        JSONObject geo = this.decompressStringToJson(restTemplate.getForObject("https://geoapi.qweather.com/v2/city/lookup?location="
                + longitude + "," + latitude + "&key=" + key, byte[].class));
        if (geo == null) return null;
        JSONObject location = geo.getJSONArray("location").getJSONObject(0);
        //取出当前城市的ID编码，方便传输
        int id = location.getInteger("id");
        String key = Const.FORUM_WEATHER_CACHE + id;
        String cache = stringRedisTemplate.opsForValue().get(key);
        if (cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        WeatherVO vo = this.fetchFromApi(id, location);
        if (vo == null) return null;
        stringRedisTemplate.opsForValue().set(key, JSONObject.from(vo).toString(), 1, TimeUnit.HOURS);
        return vo;

    }

    /**
     * 请求现在和小时的天气信息
     * @param id
     * @param location
     * @return
     */
    private WeatherVO fetchFromApi(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        JSONObject now = this.decompressStringToJson(restTemplate.getForObject(
                "https://devapi.qweather.com/v7/weather/now?location=" + id + "&key=" + key, byte[].class));
        //这里的now是整个now的data
        if (now == null) return null;
        vo.setNow(now.getJSONObject("now"));
        JSONObject hourly = this.decompressStringToJson(restTemplate.getForObject(
                "https://devapi.qweather.com/v7/weather/24h?location=" + id + "&key=" + key, byte[].class));
        if (hourly == null) return null;
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));
        return vo;
    }

    /**
     * 将string转为json
     * @param data
     * @return 解码好的json天气数据
     */
    private JSONObject decompressStringToJson(byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, read);
            }
            inputStream.close();
            stream.close();
            return JSONObject.parseObject(stream.toString());
        } catch (IOException e) {
            return null;
        }
    }
}
