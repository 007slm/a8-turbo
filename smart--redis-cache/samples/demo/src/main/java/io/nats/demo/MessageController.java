package io.nats.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@RestController
@RequestMapping("/messages")
public class MessageController {


    private final StreamBridge streamBridge;

    @Autowired
    public MessageController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @GetMapping ("/send")
    public String send(@RequestParam String message) {
        streamBridge.send("output", message);

        return "Message sent: " + message;
    }
}
