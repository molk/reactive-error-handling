package demo.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@ControllerAdvice
public class ExceptionHandlers {

	@ExceptionHandler(Exception.class)
    ResponseEntity<String> handleInternalServerError(final Exception cause) {

		log.error("an internal error occurred: {}", cause.getMessage(), cause);

		return status(INTERNAL_SERVER_ERROR)
			.contentType(MediaType.TEXT_PLAIN)
			.body(format(
				"internal error in inner service: %s\n%s",
				cause.getMessage(),
				ExceptionUtils.getStackTrace(cause)));

	}

}
