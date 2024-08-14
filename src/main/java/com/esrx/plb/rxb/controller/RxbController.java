package com.esrx.plb.rxb.controller;
import com.esrx.plb.commons.model.process.PlbIntentObject;
import com.esrx.plb.commons.process.PlbIntentFlow;
import com.esrx.plb.commons.process.PlbIntentProcessor;
import com.esrx.plb.commons.utils.PLBException;
import com.esrx.plb.rxb.impl.RxbIntentObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping(value = "/plb/v1/intent")
public class RxbController {

    @Autowired
    PlbIntentProcessor plbIntentProcessor;

    @Autowired
    PlbIntentObject rxbIntentObject;

    @PostMapping(value = "/rxb", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processRxbIntent(@RequestBody String jsonMsg) throws Exception {
        rxbIntentObject.setJsonMessage(jsonMsg);
        plbIntentProcessor.run();
        return ResponseEntity.status(HttpStatus.OK).body(jsonMsg);
    }


}

