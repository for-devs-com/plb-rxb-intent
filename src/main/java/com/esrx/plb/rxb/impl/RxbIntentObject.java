package com.esrx.plb.rxb.impl;

import com.esrx.plb.commons.model.process.PlbIntentObject;
import com.esrx.plb.commons.process.PlbIntentFunctions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.rxbxml.model.BPLHeader;
import org.rxbxml.model.PharmacyBenefits;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class RxbIntentObject extends PlbIntentObject implements PlbIntentFunctions {
    public static String[] homeDeliveryProgramValidValues = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
    public static String M_Indicator = "M";
    ObjectMapper objectMapper;

  /*  public static
    public static String Y_Indicator = "Y";*/
    HashMap<String, Object> response = new HashMap<>();

    @Override
    public void parseIntentXml() {
        log.info("Parsing Rxb Xml");
        //TODO this method should parse the XML and build the plbSetups
        try {
            if (getIntentXml() != null) {
                log.info("XML Parsing code");

                JAXBContext jaxbContext = JAXBContext.newInstance(PharmacyBenefits.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                StringReader stringReader = new StringReader(getIntentXml());
                PharmacyBenefits pharmacyBenefits = (PharmacyBenefits) unmarshaller.unmarshal(stringReader);
                if (pharmacyBenefits.getBPLs() != null && pharmacyBenefits.getBPLs().getBPLHeader().size() > 0) {
                    List<BPLHeader> bplHeaders = pharmacyBenefits.getBPLs().getBPLHeader();
                    for (BPLHeader bplHeader : bplHeaders) {
                        String homeDeliveryProgram = bplHeader.getHomeDeliveryProgram();
                        boolean isValidHomeDeliveryProgramVal = Arrays.stream(homeDeliveryProgramValidValues).anyMatch(s -> s.equals(homeDeliveryProgram));
                        if (isValidHomeDeliveryProgramVal) {
                            if (homeDeliveryProgram.equals("4")) {

                            } else if (homeDeliveryProgram.equals("5")) {

                            } else if (homeDeliveryProgram.equals("6")) {

                            } else if (homeDeliveryProgram.equals("7")) {

                            }

                        }
                    }
                }
            }
        } catch (Exception ex) {

        }
    }

    @Override
    public void buildIntentObject() {
        log.info("Building Rxb Intent object");
    }

    @Override
    public void sendIntentToMainframe() {
        log.info("Sending Rxb intent to mainframe");
    }

    @Override
    public void buildReportData() {
        log.info("Building Rxb report data");
    }

    @Override
    public void setJsonMessage(String jsonMessage) {
        log.info("Received Rxb intent");
        super.setJsonMessage(jsonMessage);
    }

    @Override
    public void customIntentFunctions() {
        log.info("Building Rxb report data");
    }
}
