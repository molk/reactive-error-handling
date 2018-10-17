package demo.service;

import lombok.Data;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Data
class Response {
    private final String value;
    private final ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
}
