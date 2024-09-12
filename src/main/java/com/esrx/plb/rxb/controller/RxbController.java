package com.esrx.plb.rxb.controller;

import com.esrx.plb.commons.model.cit.PharmacyEntityObject;
import com.esrx.plb.commons.model.process.PlbIntentObject;
import com.esrx.plb.commons.process.PlbIntentFlow;
import com.esrx.plb.commons.process.PlbIntentProcessor;
import com.esrx.plb.commons.service.HC13Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping(value = "/plb/v1/intent")
public class RxbController {

    @Autowired
    PlbIntentProcessor plbIntentProcessor;

    @Autowired
    PlbIntentObject rxbIntentObject;

    @Autowired
    PlbIntentFlow plbIntentFlow;

    @PostMapping(value = "/rxb", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> processRxbIntent(@RequestBody String jsonMsg) throws Exception {
        rxbIntentObject.setJsonMessage(jsonMsg);
        plbIntentProcessor.run();
        return ResponseEntity.status(HttpStatus.OK).body(jsonMsg);
    }

    @GetMapping(value = "/pharmacy-network-entity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> makePharmacyNetworkCall(
            @RequestParam String intentEffDate,
            @RequestParam Set<String> networkSet, @RequestParam Set<String> chainSet, @RequestParam Set<String> npiSet, @RequestBody String message) throws Exception {
        log.info("START FullIntentController.makePharmacyEntityCall( inquiryDate,chainOrNpi )");
        rxbIntentObject.setJsonMessage(message);
        rxbIntentObject.setIntentEffDate(intentEffDate);
        plbIntentFlow.setAnyPlbIntentObject(rxbIntentObject);
        PharmacyEntityObject entityObject = plbIntentFlow.getValidPharmacyEntities(networkSet, chainSet, npiSet);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(entityObject.toString());
    }


}

