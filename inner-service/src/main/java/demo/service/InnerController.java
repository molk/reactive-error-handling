package demo.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class InnerController {

    @GetMapping(path = "/retrieveData")
    public Response retrieveData() {
        return new Response(someData());
    }

    private static String someData() {
        if (raiseError()) {
            throw new RuntimeException("processing error");
        }

        return "hi there!";
    }

    private static boolean raiseError() {
        return new Random().nextBoolean();
    }

}
