package demo.service;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
class Response {
    private String value;
    private ZonedDateTime now;
}
